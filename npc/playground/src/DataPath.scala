import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

object Const {
  val PC_START = 0x80000000
}

class DatapathIO(xlen: Int) extends Bundle {
  val icache = Flipped(new ICACHEIO(xlen))
  val dcache = Flipped(new DCACHEIO(xlen))
  val ctrl = Flipped(new ControlSignals)
}

class IFIDPipelineRegister(xlen: Int) extends Bundle {
  val inst = chiselTypeOf(Instructions.NOP)
  val pc = UInt(xlen.W)
}

class IDEXPipelineRegister(xlen: Int) extends Bundle {
  val rdata1 = UInt(xlen.W)
  val rdata2 = UInt(xlen.W)
  val pc = UInt(xlen.W)
  val imm = UInt(xlen.W)
}

class EXMEMPipelineRegister(xlen: Int) extends Bundle {
  val alu = UInt(xlen.W)
  val rs1 = UInt(xlen.W)
  val rs2 = UInt(xlen.W)
  val datain = UInt(xlen.W)
}

class EXMWBPipelineRegister(xlen: Int) extends Bundle {
  val alu = UInt(xlen.W)
  val dataout = UInt(xlen.W)
}

class DataPath(val conf: CoreConfig) extends Module {
  
}