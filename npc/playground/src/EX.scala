import chisel3._
import chisel3.util._
import Control._
import Alu._
import chisel3.experimental.BundleLiterals._

class EXIO(val conf: CoreConfig) extends Bundle {
    val IDPipReg = Flipped((new IDEXPipelineRegister(conf.xlen)))

    val ex_alu_out = Output(UInt(conf.xlen.W))

    val EXPipReg = (new EXMEMPipelineRegister(conf.xlen))
}

class EXU(val conf: CoreConfig) extends Module {
    val io = IO(new EXIO(conf))

    val alu     = Module(conf.makeAlu(conf.xlen))
    val brCond  = Module(conf.makeBrCond(conf.xlen))

    io.IDPipReg.MEM <> io.EXPipReg.MEM
    io.IDPipReg.WB  <> io.EXPipReg.WB

    alu.io.A        := Mux(io.IDPipReg.EX.A_SEL === A_PC, io.IDPipReg.pc, io.IDPipReg.rdata1)
    alu.io.B        := Mux(io.IDPipReg.EX.B_SEL === B_RS2, io.IDPipReg.rdata2, io.IDPipReg.imm)
    alu.io.alu_op   := io.IDPipReg.EX.ALU_OP

    io.ex_alu_out   := alu.io.out

    brCond.io.rs1     := io.IDPipReg.rdata1
    brCond.io.rs2     := io.IDPipReg.rdata2
    brCond.io.br_type := io.IDPipReg.EX.Br_SEL

    io.EXPipReg.alu    := alu.io.out
    //exe_alu_out      := alu.io.out
    io.EXPipReg.datain := io.IDPipReg.rdata2
    io.EXPipReg.taken  := brCond.io.taken

    // debug
    io.EXPipReg.pc          := io.IDPipReg.pc
    io.EXPipReg.inst        := io.IDPipReg.inst
    io.EXPipReg.commit      := io.IDPipReg.commit
}