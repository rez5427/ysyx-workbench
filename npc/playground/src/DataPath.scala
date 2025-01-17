import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._

import Control._
import Alu._

class DataPathIO(xlen: Int) extends Bundle {
    val icache = Flipped(new ICacheIO(xlen, xlen * 2))
    val dcache = Flipped(new DCACHEIO(xlen))
}

class DataPath(val conf: CoreConfig) extends Module {
    val io      = IO(new DataPathIO(conf.xlen))

    //val ex_jmp_flg = Wire(Bool())
    //val exe_br_flg    = Wire(Bool())
    //dontTouch(exe_br_flg)

    val ifid_reg = RegInit(
      (new IFIDPipelineRegister(conf.xlen)).Lit(
        _.inst -> Instructions.NOP,
        _.pc -> 0.U,
        _.commit -> false.B
      )
    )

    val idex_reg = RegInit(
      (new IDEXPipelineRegister(conf.xlen)).Lit(
        _.rdata1 -> 0.U,
        _.rdata2 -> 0.U,
        _.pc -> 0.U,
        _.inst -> Instructions.NOP,
        _.commit -> false.B,
        _.imm -> 0.U,
        _.EX.ALU_OP -> ALU_XXX,
        _.EX.B_SEL -> B_XXX,
        _.EX.A_SEL -> A_XXX,
        _.EX.Br_SEL -> Br_XXX,
        _.MEM.Mask -> 0.U,
        _.MEM.Wen -> 0.U,
        _.MEM.PC_SEL -> PC_4,
        _.MEM.MemToReg -> WB_XXX,
        _.WB.RegWr -> 0.U,
        _.WB.Rd -> 0.U
      )
    )

    val exmem_reg = RegInit(
      (new EXMEMPipelineRegister(conf.xlen)).Lit(
        _.alu -> 0.U,
        _.datain -> 0.U,
        _.taken -> false.B,
        _.pc -> 0.U,
        _.inst -> Instructions.NOP,
        _.commit -> false.B,
        _.MEM.Mask -> 0.U,
        _.MEM.Wen -> 0.U,
        _.MEM.PC_SEL -> PC_4,
        //_.WB.MemToReg -> WB_XXX,
        _.WB.RegWr -> 0.U,
        _.WB.Rd -> 0.U
      )
    )

    val memwb_reg = RegInit(
      (new MEMWBPipelineRegister(conf.xlen)).Lit(
        _.data -> 0.U,
        _.commit -> false.B,
        _.pc -> 0.U,
        _.inst -> Instructions.NOP,
        //_.WB.MemToReg -> WB_XXX,
        _.WB.RegWr -> 0.U,
        _.WB.Rd -> 0.U
      )
    )

    val IFU = Module(new IFU(conf))
    val IDU = Module(new IDU(conf))
    val EXU = Module(new EXU(conf))
    val MEM = Module(new MEM(conf))
    val WBU = Module(new WB(conf))

    val regFile = Module(new RegFile(conf.xlen))

    regFile.io.Ra     := IDU.io.IFPipReg.inst(19, 15)
    regFile.io.Rb     := IDU.io.IFPipReg.inst(24, 20)
    regFile.io.Rw     := memwb_reg.WB.Rd
    regFile.io.RegWr  := memwb_reg.WB.RegWr

    /** **** IF stage ****
      */

    IFU.io.icache <> io.icache

    IFU.io.exe_alu_out    := EXU.io.ex_alu_out
    //IFU.io.stall          := stall_flg
    IFU.io.taken          := EXU.io.EXPipReg.taken
    IFU.io.idex_pc_sel    := EXU.io.EXPipReg.MEM.PC_SEL

    IFU.io.IFPipReg <> ifid_reg
    //dontTouch(ifid_reg)

    /** **** ID stage ****
      */

    IDU.io.IFPipReg <> ifid_reg

    //IDU.io.busW           := MEM.io.WBPipReg.WB.Rd
    //IDU.io.RegWr          := MEM.io.WBPipReg.WB.RegWr
    IDU.io.MemALUOut      := MEM.io.WBPipReg.data
    IDU.io.MemALURegOut   := memwb_reg.data

    IDU.io.busA := regFile.io.busA
    IDU.io.busB := regFile.io.busB

    IDU.io.mem_addr := MEM.io.WBPipReg.WB.Rd
    IDU.io.mem_reg_addr  := memwb_reg.WB.Rd

    IDU.io.mem_wen := MEM.io.WBPipReg.WB.RegWr
    IDU.io.mem_reg_wen  := memwb_reg.WB.RegWr

    IDU.io.IDPipReg <> idex_reg

    IDU.io.exe_reg_wen := idex_reg.WB.RegWr
    IDU.io.exe_reg_addr := idex_reg.WB.Rd

    /** **** EX stage ****
      */
    
    EXU.io.IDPipReg <> idex_reg
    idex_reg <> IDU.io.IDPipReg

    EXU.io.EXPipReg <> exmem_reg
    
    /** **** MEM stage ****
      */

    MEM.io.EXPipReg <> exmem_reg

    memwb_reg <> WBU.io.WBPipReg

    io.dcache.addr  := EXU.io.EXPipReg.alu
    io.dcache.wdata := EXU.io.EXPipReg.datain
    io.dcache.mask  := EXU.io.EXPipReg.MEM.Mask
    io.dcache.wen   := EXU.io.EXPipReg.MEM.Wen

    MEM.io.DcacheData := io.dcache.data

    MEM.io.WBPipReg <> memwb_reg

    /** **** WB stage ****
      */

    WBU.io.WBPipReg <> memwb_reg
    //IDU.io.busW := WBU.io.busw
    WBU.io.regs := regFile.io.regs

    regFile.io.busW := 0.U
    when(memwb_reg.WB.RegWr === Wen_Y) {
      regFile.io.busW := memwb_reg.data
    }
}
