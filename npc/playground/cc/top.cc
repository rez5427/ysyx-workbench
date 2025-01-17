#include <verilated.h>
#if VM_TRACE
# include <verilated_vcd_c.h>	// Trace file format header
#endif

#include <vector>
#include "./include/monitor/sdb.h"
#include "dlfcn.h"

#include <sys/socket.h>
#include <arpa/inet.h>

int sockfd;

typedef struct {
    uint32_t rvfi_insn; // [0 - 3] Instruction word (32-bit instruction or command)
    uint16_t rvfi_time; // [5 - 4] Time to inject the token
    uint8_t rvfi_cmd;   // [6] Trace command
    uint8_t padding;    // [7] Padding byte
} RVFI_DII_Instruction_Packet;

typedef struct {
    uint64_t rvfi_order;     // [00 - 07] Instruction number
    uint64_t rvfi_pc_rdata;  // [08 - 15] PC before instruction
    uint64_t rvfi_pc_wdata;  // [16 - 23] PC after instruction
    uint64_t rvfi_insn;      // [24 - 31] Instruction word
    uint64_t rvfi_rs1_data;  // [32 - 39] Read register value 1
    uint64_t rvfi_rs2_data;  // [40 - 47] Read register value 2
    uint64_t rvfi_rd_wdata;  // [48 - 55] Write register value
    uint64_t rvfi_mem_addr;  // [56 - 63] Memory access address
    uint64_t rvfi_mem_rdata; // [64 - 71] Memory read data
    uint64_t rvfi_mem_wdata; // [72 - 79] Memory write data
    uint8_t  rvfi_mem_rmask; // [80]      Read mask
    uint8_t  rvfi_mem_wmask; // [81]      Write mask
    uint8_t  rvfi_rs1_addr;  // [82]      Read register address 1
    uint8_t  rvfi_rs2_addr;  // [83]      Read register address 2
    uint8_t  rvfi_rd_addr;   // [84]      Write register address
    uint8_t  rvfi_trap;      // [85]      Trap indicator
    uint8_t  rvfi_halt;      // [86]      Halt indicator
    uint8_t  rvfi_intr;      // [87]      Interrupt indicator
} RVFI_DII_Execution_Packet;

static int count = 0;

typedef struct {
  uint32_t gpr[32];
  uint32_t pc;

  /* CSRs */
  uintptr_t mepc;
  uintptr_t mcause;
  uintptr_t mstatus;

  uintptr_t mtvec;

  uint64_t priv;

} riscv32_CPU_state;

#define RESET_VECTOR 0x20000000

typedef void (*difftest_exec_t)(void *npc_cpu);
difftest_exec_t difftest_exec;

typedef void (*difftest_regcpy)(void *dut, bool direction);


void load_mem(uint8_t* mem, const char* fn);

VysyxSoCFull* top;

vluint64_t main_time = 0; 

#define MROMSHIFT 0x20000000

static uint8_t mem[0x4000] = {0};

extern "C" void flash_read(uint32_t addr, uint32_t *data) { 
  printf("flash_read: addr = %08x\n", addr);
}
extern "C" void mrom_read(uint32_t addr, uint32_t *data) { 
  //printf("mrom_read: addr = %08x\n", addr);
  *data = mem[addr - MROMSHIFT + 3] << 24 | mem[addr + 2 - MROMSHIFT] << 16 | mem[addr + 1 - MROMSHIFT] << 8 | mem[addr - MROMSHIFT];
  //printf("addr - MROMSHIFT: %x\n", addr - MROMSHIFT);
  //printf("mrom_read: data = %02x %02x %02x %02x\n", mem[addr - MROMSHIFT], mem[addr + 1 - MROMSHIFT], mem[addr + 2 - MROMSHIFT], mem[addr + 3 - MROMSHIFT]);
  //printf("data: %08x\n", *data);
}

int last_pc = 0;

extern "C" void trigger_nemu_difftest(int commit, int regs[32], int pc, int inst, int busw, int Rd, int RegWr) { 
  if(commit) {
    if(count == 0) {
      count++;
      return;
    }
    if(pc == last_pc) {
      return;
    }
    riscv32_CPU_state cpu_state;
    for(int i = 0; i < 32; i++) {
      cpu_state.gpr[i] = regs[i];
    }
    cpu_state.pc = pc;

    if(RegWr) {
      cpu_state.gpr[Rd] = busw;
    }

    difftest_exec(&cpu_state);

    last_pc = pc;
    count++;
  }
}

extern "C" void trigger_info_difftest(int commit, int regs[32], int pc, int inst) { 
  if(commit) {
    printf("pc: %08x\n", pc);
    printf("inst: %08x\n", inst);
  }
}

extern "C" void trigger_difftest(int commit, int regs[32], int pc, int inst) { 
  if(commit) {
    RVFI_DII_Instruction_Packet packet;
    packet.padding = 0;
    packet.rvfi_cmd = 1;
    packet.rvfi_time = 0;
    packet.rvfi_insn = inst;
    ssize_t num_bytes = send(sockfd, &packet, sizeof(packet), 0);
    
    if (num_bytes < 0) {
        perror("recv");
        exit(EXIT_FAILURE);
    } else if (num_bytes == 0) {
        printf("Connection closed by peer.\n");
        return;
    }

    RVFI_DII_Execution_Packet exec_packet;
    num_bytes = send(sockfd, &exec_packet, sizeof(exec_packet), 0);
    if (num_bytes < 0) {
        perror("recv");
        exit(EXIT_FAILURE);
    } else if (num_bytes == 0) {
        printf("Connection closed by peer.\n");
        return;
    }
  }
}

#ifdef VM_TRACE
VerilatedVcdC* tfp;
#endif

static void single_cycle() {
  top->clock = 1; top->eval();
#if VM_TRACE
  if (tfp) tfp->dump((double) main_time);
#endif // VM_TRACE
main_time++;
  top->clock = 0; top->eval();
#if VM_TRACE
  if (tfp) tfp->dump((double) main_time);
#endif // VM_TRACE
main_time++;
}

static void reset(int n) {
  top->reset = 1;
  while (n -- > 0) single_cycle();
  top->reset = 0;
}

int main(int argc, char** argv) {
  Verilated::commandArgs(argc, argv); 
  top = new VysyxSoCFull; // target design
  load_mem(mem, argv[1]);

#if VM_TRACE	
  tfp = new VerilatedVcdC;
  Verilated::traceEverOn(true);
  top->trace(tfp, 0);
  tfp->open("dump.vcd");
#endif
  reset(10);

  init_sdb();

  void *handle = dlopen("/home/rez/workbench/ics2023/nemu/build/riscv32-nemu-interpreter-so", RTLD_LAZY);

  if(handle==NULL)
    printf("dlopen error\n");

  typedef void (*difftest_memcpy_t)(uint32_t, void*, size_t, bool);

  difftest_memcpy_t difftest_memcpy = (difftest_memcpy_t) dlsym(handle, "difftest_memcpy");

  difftest_memcpy(RESET_VECTOR, mem, 0x4000, true);

  difftest_exec = (difftest_exec_t) dlsym(handle, "difftest_exec");

  while(1000) {
    //sdb_mainloop();
    single_cycle();
  }

  tfp->close();
}
