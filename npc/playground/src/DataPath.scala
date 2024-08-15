import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._
import Control._
import Alu._

object Const {
  val PC_START = 0x20000000
}

class DataPathIO(xlen: Int) extends Bundle {
  val icache = Flipped(new ICacheIO(xlen, xlen * 2))
  val dcache = Flipped(new DCACHEIO(xlen))
  val ctrl   = Flipped(new ControlSignals)
}

class WBRegister(xlen: Int) extends Bundle {
  val MemToReg = UInt(2.W)
  val RegWr    = UInt(1.W)
  val Rd       = UInt(5.W)
}

class MEMRegister(xlen: Int) extends Bundle {
  val Mask   = UInt(4.W)
  val Wen    = UInt(1.W)
  val PC_SEL = UInt(2.W)
}

class EXRegister(xlen: Int) extends Bundle {
  val ALU_OP = UInt(4.W)
  val B_SEL  = UInt(1.W)
  val A_SEL  = UInt(1.W)
  val Br_SEL = UInt(3.W)
  val kill   = Bool()
}

class IFIDPipelineRegister(xlen: Int) extends Bundle {
  val inst = chiselTypeOf(Instructions.NOP)
  val pc   = UInt(xlen.W)
}

class IDEXPipelineRegister(xlen: Int) extends Bundle {
  val rdata1 = UInt(xlen.W)
  val rdata2 = UInt(xlen.W)
  val pc     = UInt(xlen.W)
  val imm    = UInt(xlen.W)

  val EX_Reg = new EXRegister(xlen)

  val MEM_Reg = new MEMRegister(xlen)

  val WB_Reg = new WBRegister(xlen)
}

class EXMEMPipelineRegister(xlen: Int) extends Bundle {
  val alu    = UInt(xlen.W)
  val taken  = Bool()
  val datain = UInt(xlen.W)

  val MEM_Reg = new MEMRegister(xlen)

  val WB_Reg = new WBRegister(xlen)
}

class MEMWBPipelineRegister(xlen: Int) extends Bundle {
  val data = UInt(xlen.W)

  val WB_Reg = new WBRegister(xlen)
}

class DataPath(val conf: CoreConfig) extends Module {
  val io      = IO(new DataPathIO(conf.xlen))
  val regFile = Module(new RegFile(conf.xlen))
  val alu     = Module(conf.makeAlu(conf.xlen))
  val immGen  = Module(conf.makeImmGen(conf.xlen))
  val brCond  = Module(conf.makeBrCond(conf.xlen))

  val ex_jmp_flg = Wire(Bool())
  //val exe_br_flg    = Wire(Bool())
  //dontTouch(exe_br_flg)

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
      _.imm -> 0.U,
      _.EX_Reg.ALU_OP -> ALU_XXX,
      _.EX_Reg.B_SEL -> B_XXX,
      _.EX_Reg.A_SEL -> A_XXX,
      _.EX_Reg.Br_SEL -> Br_XXX,
      _.MEM_Reg.Mask -> 0.U,
      _.MEM_Reg.Wen -> 0.U,
      _.MEM_Reg.PC_SEL -> PC_4,
      _.WB_Reg.MemToReg -> WB_XXX,
      _.WB_Reg.RegWr -> 0.U,
      _.WB_Reg.Rd -> 0.U
    )
  )

  val exmem_reg = RegInit(
    (new EXMEMPipelineRegister(conf.xlen)).Lit(
      _.alu -> 0.U,
      _.datain -> 0.U,
      _.taken -> false.B,
      _.MEM_Reg.Mask -> 0.U,
      _.MEM_Reg.Wen -> 0.U,
      _.MEM_Reg.PC_SEL -> PC_4,
      _.WB_Reg.MemToReg -> WB_XXX,
      _.WB_Reg.RegWr -> 0.U,
      _.WB_Reg.Rd -> 0.U
    )
  )

  val memwb_reg = RegInit(
    (new MEMWBPipelineRegister(conf.xlen)).Lit(
      _.data -> 0.U,
      _.WB_Reg.MemToReg -> WB_XXX,
      _.WB_Reg.RegWr -> 0.U,
      _.WB_Reg.Rd -> 0.U
    )
  )

  /** **** IF stage ****
    */
  val stall_flg   = !io.icache.resp.valid
  val exe_alu_out = Wire(UInt(conf.xlen.W))
  val mem_wb_data = Wire(UInt(conf.xlen.W))

  val started = RegNext(reset.asBool)
  val pc      = RegInit(Const.PC_START.U - 4.U)


  val if_inst = Mux(started || io.ctrl.kill || brCond.io.taken, Instructions.NOP, io.icache.resp.bits.data)
  

  val pc_next = MuxCase(
    pc + 4.U,
    IndexedSeq(
      ((brCond.io.taken) || (ex_jmp_flg) || (idex_reg.MEM_Reg.PC_SEL === PC_ALU)) -> (alu.io.out >> 1.U << 1.U),
      stall_flg -> pc
    )
  )
  pc := pc_next
  io.icache.req.bits.addr := pc_next
  io.icache.req.valid := !stall_flg

  when(!stall_flg) {
    ifid_reg.pc := pc
    ifid_reg.inst := if_inst
  }

  /** **** ID stage ****
    */

  val rs1_addr = ifid_reg.inst(19, 15)
  val rs2_addr = ifid_reg.inst(24, 20)

  val id_rs1_data_hazard =
    (idex_reg.WB_Reg.RegWr === Wen_Y) && (idex_reg.WB_Reg.Rd =/= 0.U) && (rs1_addr === idex_reg.WB_Reg.Rd)
  val id_rs2_data_hazard =
    (idex_reg.WB_Reg.RegWr === Wen_Y) && (idex_reg.WB_Reg.Rd =/= 0.U) && (rs2_addr === idex_reg.WB_Reg.Rd)

  val id_inst = ifid_reg.inst
  io.ctrl.inst := ifid_reg.inst

  idex_reg.EX_Reg.ALU_OP := io.ctrl.alu_op
  idex_reg.EX_Reg.B_SEL  := io.ctrl.B_sel
  idex_reg.EX_Reg.A_SEL  := io.ctrl.A_sel
  idex_reg.EX_Reg.Br_SEL := io.ctrl.br_type

  idex_reg.MEM_Reg.Mask   := io.ctrl.mask
  idex_reg.MEM_Reg.Wen    := io.ctrl.wen
  idex_reg.MEM_Reg.PC_SEL := io.ctrl.pc_sel

  idex_reg.WB_Reg.MemToReg := io.ctrl.wb_sel
  idex_reg.WB_Reg.RegWr    := io.ctrl.reg_wr
  idex_reg.WB_Reg.Rd       := id_inst(11, 7)

  regFile.io.Ra    := id_inst(19, 15)
  regFile.io.Rb    := id_inst(24, 20)
  regFile.io.Rw    := memwb_reg.WB_Reg.Rd
  regFile.io.RegWr := memwb_reg.WB_Reg.RegWr

  immGen.io.inst := id_inst
  immGen.io.sel  := io.ctrl.imm_sel

  idex_reg.rdata1 := MuxCase(
    regFile.io.busA,
    Seq(
      ((rs1_addr === exmem_reg.WB_Reg.Rd) && (exmem_reg.WB_Reg.RegWr === Wen_Y)) -> mem_wb_data,
      ((rs1_addr === memwb_reg.WB_Reg.Rd) && (memwb_reg.WB_Reg.RegWr === Wen_Y)) -> memwb_reg.data
    )
  )
  idex_reg.rdata2 := MuxCase(
    regFile.io.busB,
    Seq(
      ((rs2_addr === exmem_reg.WB_Reg.Rd) && (exmem_reg.WB_Reg.RegWr === Wen_Y)) -> mem_wb_data,
      ((rs2_addr === memwb_reg.WB_Reg.Rd) && (memwb_reg.WB_Reg.RegWr === Wen_Y)) -> memwb_reg.data
    )
  )
  idex_reg.pc  := ifid_reg.pc
  idex_reg.imm := immGen.io.out

  /** **** EX stage ****
    */

  exmem_reg.MEM_Reg := idex_reg.MEM_Reg
  exmem_reg.WB_Reg  := idex_reg.WB_Reg

  alu.io.A      := Mux(idex_reg.EX_Reg.A_SEL === A_PC, idex_reg.pc, idex_reg.rdata1)
  alu.io.B      := Mux(idex_reg.EX_Reg.B_SEL === B_RS2, idex_reg.rdata2, idex_reg.imm)
  alu.io.alu_op := idex_reg.EX_Reg.ALU_OP

  brCond.io.rs1     := idex_reg.rdata1
  brCond.io.rs2     := idex_reg.rdata2
  brCond.io.br_type := idex_reg.EX_Reg.Br_SEL

  exmem_reg.alu    := alu.io.out
  exe_alu_out      := alu.io.out
  exmem_reg.datain := idex_reg.rdata2

  ex_jmp_flg := (idex_reg.WB_Reg.MemToReg === WB_PC4)

  /** **** MEM stage ****
    */

  memwb_reg.WB_Reg := exmem_reg.WB_Reg

  io.dcache.addr  := exmem_reg.alu
  io.dcache.wdata := exmem_reg.datain
  io.dcache.mask  := exmem_reg.MEM_Reg.Mask
  io.dcache.wen   := exmem_reg.MEM_Reg.Wen

  mem_wb_data := MuxCase(
    exmem_reg.alu,
    Seq(
      (exmem_reg.WB_Reg.MemToReg === WB_MEM) -> io.dcache.data
    )
  )

  memwb_reg.data := mem_wb_data

  /** **** WB stage ****
    */

  regFile.io.busW := 0.U
  when(memwb_reg.WB_Reg.RegWr === Wen_Y) {
    regFile.io.busW := memwb_reg.data
  }
}
