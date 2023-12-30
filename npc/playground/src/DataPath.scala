import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

object Const {
  val PC_START = 0x80000000
}

class DataPathIO(xlen: Int) extends Bundle {
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

class MEMWBPipelineRegister(xlen: Int) extends Bundle {
  val alu = UInt(xlen.W)
  val dataout = UInt(xlen.W)
}

class DataPath(val conf: CoreConfig) extends Module {
  val io = IO(new DataPathIO(conf.xlen))
  val regFile = Module(new RegFile(conf.xlen))
  val alu = Module(conf.makeAlu(conf.xlen))
  val immGen = Module(conf.makeImmGen(conf.xlen))
  val brCond = Module(conf.makeBrCond(conf.xlen))

  import Control._

  val ifid_reg = RegInit(
    (new IFIDPipelineRegister(conf.xlen)).Lit(
      _.inst -> Instructions.NOP,
      _.pc -> 0.U
    )
  )

  val idex_reg = RegInit(
    (new IDEXPipelineRegister(conf.xlen)).Lit(
      _.rdata1 -> 0.U,
      _.rdata2 -> 0.U,
      _.pc -> 0.U,
      _.imm -> 0.U
    )
  )

  val exmem_reg = RegInit(
    (new EXMEMPipelineRegister(conf.xlen)).Lit(
      _.alu -> 0.U,
      _.datain -> 0.U,
      _.rs1 -> 0.U,
      _.rs2 -> 0.U
    )
  )

  val memwb_reg = RegInit(
    (new MEMWBPipelineRegister(conf.xlen)).Lit(
      _.alu -> 0.U,
      _.dataout -> 0.U
    )
  )

  /** **** IF stage **** */
  val pc = RegInit(Const.PC_START.U(conf.xlen.W))
  val pc_next  = MuxCase(
    pc + 4.U,
    Seq(
      ((io.ctrl.pc_sel === PC_ALU) || (brCond.io.taken)) -> (alu.io.out >> 1.U << 1.U),
    )
  )

  pc := pc_next

  io.icache.addr := pc_next
  

  ifid_reg.pc := pc
  ifid_reg.inst := io.icache.inst

  /** **** ID stage **** */

  io.ctrl.inst := ifid_reg.inst

  val rs1_addr = ifid_reg.inst(19, 15)
  val rs2_addr = ifid_reg.inst(24, 20)
  val rb_addr = ifid_reg.inst(11, 7)

  regFile.io.Ra := rs1_addr
  regFile.io.Rb := rs2_addr
  regFile.io.RegWr := io.ctrl.wb_en

  immGen.io.inst := ifid_reg.inst
  immGen.io.sel := io.ctrl.imm_sel

  idex_reg.rdata1 := regFile.io.busA
  idex_reg.rdata2 := regFile.io.busB
  idex_reg.pc := ifid_reg.pc
  idex_reg.imm := immGen.io.out

  /** **** EX stage **** */

  alu.io.A := Mux(io.ctrl.A_sel === A_PC, idex_reg.pc, idex_reg.rdata1)
  alu.io.B := Mux(io.ctrl.B_sel === B_RS2, idex_reg.rdata2, idex_reg.imm)
  alu.io.alu_op := io.ctrl.alu_op

  exmem_reg.alu := alu.io.out
  exmem_reg.datain := idex_reg.rdata2
  exmem_reg.rs1 := Mux(io.ctrl.A_sel === A_PC, idex_reg.pc, idex_reg.rdata1)
  exmem_reg.rs2 := Mux(io.ctrl.B_sel === B_RS2, idex_reg.rdata2, idex_reg.imm)

  /** **** MEM stage **** */

  brCond.io.rs1 := exmem_reg.rs1
  brCond.io.rs2 := exmem_reg.rs2
  brCond.io.br_type := io.ctrl.br_type

  io.dcache.addr := exmem_reg.alu
  io.dcache.wdata := exmem_reg.datain
  io.dcache.mask := io.ctrl.mask
  io.dcache.wen := io.ctrl.wb_en

  memwb_reg.alu := exmem_reg.alu
  memwb_reg.dataout := io.dcache.data

  /** **** WB stage **** */

  regFile.io.busW := Mux(io.ctrl.wb_sel === WB_ALU, memwb_reg.alu, memwb_reg.dataout)
}