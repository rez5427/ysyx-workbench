import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

class WBRegister(xlen: Int) extends Bundle {
  //val MemToReg = UInt(2.W)
  val RegWr    = UInt(1.W)
  val Rd       = UInt(5.W)
}

class MEMRegister(xlen: Int) extends Bundle {
  val Mask      = UInt(4.W)
  val Wen       = UInt(1.W)
  val PC_SEL    = UInt(2.W)
  val MemToReg  = UInt(2.W)
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

  /* debug signal*/
  val commit      = Bool()
  /* debug signal end*/
}

object IFIDPipelineRegister {
  def apply(xlen: Int): IFIDPipelineRegister = {
    val reg = Wire(new IFIDPipelineRegister(xlen))
    reg.inst := Instructions.NOP
    reg.pc := 0.U
    reg.commit := false.B
    reg
  }
}

class IDEXPipelineRegister(xlen: Int) extends Bundle {
  val rdata1 = UInt(xlen.W)
  val rdata2 = UInt(xlen.W)

  /* debug signal*/
  val pc          = UInt(xlen.W)
  val inst        = UInt(xlen.W)
  val commit      = Bool()
  /* debug signal end*/

  val imm         = UInt(xlen.W)

  val EX          = new EXRegister(xlen)
  val MEM         = new MEMRegister(xlen)
  val WB          = new WBRegister(xlen)
}

object IDEXPipelineRegister {
  def apply(xlen: Int): IDEXPipelineRegister = {
    val reg = Wire(new IDEXPipelineRegister(xlen))
    reg.rdata1 := 0.U
    reg.rdata2 := 0.U

    // debug
    reg.pc := 0.U
    reg.commit := false.B
    reg.inst := 0.U

    reg.imm := 0.U

    reg.EX := Wire(new EXRegister(xlen)).Lit(
      _.ALU_OP -> 0.U,
      _.B_SEL -> 0.U,
      _.A_SEL -> 0.U,
      _.Br_SEL -> 0.U,
      _.kill -> false.B
    )

    reg.MEM := Wire(new MEMRegister(xlen)).Lit(
      _.Mask -> 0.U,
      _.Wen -> 0.U,
      _.PC_SEL -> 0.U
    )

    reg.WB := Wire(new WBRegister(xlen)).Lit(
      //_.MemToReg -> 0.U,
      _.RegWr -> 0.U,
      _.Rd -> 0.U
    )

    reg
  }
}

class EXMEMPipelineRegister(xlen: Int) extends Bundle {
  val alu    = UInt(xlen.W)
  val taken  = Bool()
  val datain = UInt(xlen.W)

  /* debug signal*/
  val pc          = UInt(xlen.W)
  val inst        = UInt(xlen.W)
  val commit      = Bool()
  /* debug signal end*/

  val MEM = new MEMRegister(xlen)

  val WB = new WBRegister(xlen)
}

object EXMEMPipelineRegister {
  def apply(xlen: Int): EXMEMPipelineRegister = {
    val reg = Wire(new EXMEMPipelineRegister(xlen))
    reg.alu := 0.U
    reg.taken := false.B
    reg.datain := 0.U

    //debug
    reg.pc := 0.U
    reg.inst := 0.U
    reg.commit := false.B

    reg.MEM := Wire(new MEMRegister(xlen)).Lit(
      _.Mask -> 0.U,
      _.Wen -> 0.U,
      _.PC_SEL -> 0.U
    )

    reg.WB := Wire(new WBRegister(xlen)).Lit(
      //_.MemToReg -> 0.U,
      _.RegWr -> 0.U,
      _.Rd -> 0.U
    )

    reg
  }
}

class MEMWBPipelineRegister(xlen: Int) extends Bundle {
  val data = UInt(xlen.W)

  /* debug signal*/
  val pc          =   UInt(xlen.W)
  val inst        =   UInt(xlen.W)
  val commit      =   Bool()
  /* debug signal end*/
  
  val WB = new WBRegister(xlen)
}

object MEMWBPipelineRegister {
  def apply(xlen: Int): MEMWBPipelineRegister = {
    val reg = Wire(new MEMWBPipelineRegister(xlen))
    reg.data := 0.U

    //debug
    reg.pc := 0.U
    reg.inst := 0.U
    reg.commit := false.B

    reg.WB := Wire(new WBRegister(xlen)).Lit(
      //_.MemToReg -> 0.U,
      _.RegWr -> 0.U,
      _.Rd -> 0.U
    )

    reg
  }
}