import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFile

class ICACHEIO(xlen: Int) extends Bundle {
    val addr = Input(UInt(xlen.W))
    val inst = Output(UInt(xlen.W))
}

class ICACHE(xlen: Int) extends Module {
    val io = IO(new ICACHEIO(xlen))

    val icache = Mem(1024, UInt(8.W))
    loadMemoryFromFile(icache, "soft/program/bubble-sort-riscv32-nemu.bin")

    io.inst := Cat(
        icache(io.addr + 3.U), 
        icache(io.addr + 2.U), 
        icache(io.addr + 1.U), 
        icache(io.addr)
    )
}

class DCACHEIO(xlen: Int) extends Bundle {
    val addr = Input(UInt(xlen.W))
    val data = Output(UInt(xlen.W))
    val wen = Input(Bool())
    val wdata = Input(UInt(xlen.W))
    val mask = Input(UInt(4.W))
}

class DCACHE(xlen: Int) extends Module {
    val io = IO(new DCACHEIO(xlen))

    val dcache = Mem(1024, UInt(8.W))
    
    when(io.wen) {
        when(io.mask(0)) {
            dcache(io.addr) := io.wdata(7, 0)
        }
        when(io.mask(1)) {
            dcache(io.addr + 1.U) := io.wdata(15, 8)
        }
        when(io.mask(2)) {
            dcache(io.addr + 2.U) := io.wdata(23, 16)
        }
        when(io.mask(3)) {
            dcache(io.addr + 3.U) := io.wdata(31, 24)
        }
    }

    io.data := Cat(
        dcache(io.addr + 3.U), 
        dcache(io.addr + 2.U), 
        dcache(io.addr + 1.U), 
        dcache(io.addr)
    )
}