import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

import Control._

class IDIO(val conf: CoreConfig) extends Bundle {
    val IFPipReg = Flipped((new IFIDPipelineRegister(conf.xlen)))

    //val busW    = Input(UInt(5.W))
    //val RegWr = Input(Bool())

    val busA = Input(UInt(conf.xlen.W))
    val busB = Input(UInt(conf.xlen.W))

    val MemALUOut = Input(UInt(conf.xlen.W))
    val MemALURegOut = Input(UInt(conf.xlen.W))

    // For data hazard benefit
    // If exu needs the data write regfile in mem stage
    // then Move the data from the MEM stage to the EXU.
    val mem_addr = Input(UInt(5.W))
    // Same as above but for the write back stage
    val mem_reg_addr = Input(UInt(5.W))

    val mem_wen = Input(Bool())
    val mem_reg_wen = Input(Bool())

    val exe_reg_wen = Input(Bool())
    val exe_reg_addr = Input(UInt(5.W))

    val IDPipReg = (new IDEXPipelineRegister(conf.xlen))
}

class IDU(val conf: CoreConfig) extends Module {
    val io = IO(new IDIO(conf))

    val ctrl   = Module(new Control)

    //val regFile = Module(new RegFile(conf.xlen))
    val immGen  = Module(conf.makeImmGen(conf.xlen))
    
    /* unsettled
    regFile.io.Ra       := id_inst(19, 15)
    regFile.io.Rb       := id_inst(24, 20)
    regFile.io.Rw       := id_inst(11, 7)
    regFile.io.RegWr    := io.RegWr
    regFile.io.busW     := io.busW
    */

    val id_rs1_data_hazard = (io.exe_reg_wen === Wen_Y) && (io.IFPipReg.inst(19, 15) =/= 0.U) && (io.IFPipReg.inst(19, 15) === io.exe_reg_addr)
    val id_rs2_data_hazard = (io.exe_reg_wen === Wen_Y) && (io.IFPipReg.inst(24, 20) =/= 0.U) && (io.IFPipReg.inst(24, 20) === io.exe_reg_addr)

    val id_inst = Mux((id_rs1_data_hazard || id_rs2_data_hazard), Instructions.NOP, io.IFPipReg.inst) 

    val rs1_addr = id_inst(19, 15)
    val rs2_addr = id_inst(24, 20)
    val rd_addr  = id_inst(11, 7)

    dontTouch(rs1_addr)
    dontTouch(rs2_addr)

    dontTouch(id_rs1_data_hazard)
    dontTouch(id_rs2_data_hazard) 

    ctrl.io.inst := id_inst

    immGen.io.inst := id_inst
    immGen.io.sel  := ctrl.io.imm_sel

    io.IDPipReg.EX.ALU_OP   := ctrl.io.alu_op
    io.IDPipReg.EX.B_SEL    := ctrl.io.B_sel
    io.IDPipReg.EX.A_SEL    := ctrl.io.A_sel
    io.IDPipReg.EX.Br_SEL   := ctrl.io.br_type
    io.IDPipReg.EX.kill     := false.B

    io.IDPipReg.MEM.Mask    := ctrl.io.mask
    io.IDPipReg.MEM.Wen     := ctrl.io.wen
    io.IDPipReg.MEM.PC_SEL  := ctrl.io.pc_sel
    io.IDPipReg.MEM.MemToReg := ctrl.io.wb_sel

    io.IDPipReg.WB.RegWr    := ctrl.io.reg_wr
    io.IDPipReg.WB.Rd       := rd_addr

    io.IDPipReg.rdata1      := MuxCase(
        io.busA,
        Seq(
        ((rs1_addr === io.mem_addr) && (io.mem_wen === Wen_Y)) -> io.MemALUOut,
        ((rs1_addr === io.mem_reg_addr) && (io.mem_reg_wen === Wen_Y)) -> io.MemALURegOut
        )
    )

    io.IDPipReg.rdata2      := MuxCase(
        io.busB,
        Seq(
        ((rs2_addr === io.mem_addr) && (io.mem_wen === Wen_Y)) -> io.MemALUOut,
        ((rs2_addr === io.mem_reg_addr) && (io.mem_reg_wen === Wen_Y)) -> io.MemALURegOut
        )
    )

    io.IDPipReg.imm         := immGen.io.out

    // debug
    io.IDPipReg.commit      := io.IFPipReg.commit
    io.IDPipReg.pc          := io.IFPipReg.pc
    io.IDPipReg.inst        := io.IFPipReg.inst
}