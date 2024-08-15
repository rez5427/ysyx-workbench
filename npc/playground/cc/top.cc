#include <verilated.h>
#if VM_TRACE
# include <verilated_vcd_c.h>	// Trace file format header
#endif

#include <vector>
#include "./include/monitor/sdb.h"
#include "dlfcn.h"

using namespace std;

#define RESET_VECTOR 0x80000000



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

  while(true) {
    sdb_mainloop();
  }

  tfp->close();
}
