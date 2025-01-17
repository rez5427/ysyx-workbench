import chisel3._
import chisel3.util._
import chisel3.experimental.BundleLiterals._
import Control._

class MEMIO(xlen: Int) extends Bundle {
    val EXPipReg = Flipped((new EXMEMPipelineRegister(xlen)))

    //val dcache = Flipped(new DCACHEIO(xlen))

    val DcacheData = Input(UInt(xlen.W))

    // For data hazard benefit
    val MemALUOut = Output(UInt(xlen.W))

    val WBPipReg = (new MEMWBPipelineRegister(xlen))
}

class MEM(val conf: CoreConfig) extends Module {
    val io = IO(new MEMIO(conf.xlen))

    /*
        Pipline Handshake

        -----------             -----------             -----------
        |         |    valid    |         |    valid    |         |
        |         |-----------> |         |-----------> |         |
        |   E X   |             |   MEM   |             |   W B   |
        |         |    ready    |         |    ready    |         |
        |         |<----------- |         |<----------- |         |
        -----------             -----------             -----------
    

    val mem_valid = RegInit(0.U(1.W))

    val mem_bus_reg = RegInit(
        (new EXMEMPipelineRegister(conf.xlen)).Lit(
        _.alu -> 0.U,
        _.datain -> 0.U,
        _.taken -> false.B,
        _.pc -> 0.U,
        _.MEM.Mask -> 0.U,
        _.MEM.Wen -> 0.U,
        _.MEM.PC_SEL -> PC_4,
        _.WB.MemToReg -> WB_XXX,
        _.WB.RegWr -> 0.U,
        _.WB.Rd -> 0.U
        )
    )

    io.EXPipReg.ready := !mem_valid || io.WBPipReg.ready

    when(io.EXPipReg.ready) {
        mem_valid := ~reset.asBool
    }

    when(io.EXPipReg.valid && io.EXPipReg.ready) {
        mem_bus_reg := io.EXPipReg
    }

    io.WBPipReg.valid := mem_valid

    
        Pipline Handshake finished
    */

    io.EXPipReg.WB <> io.WBPipReg.WB

    io.MemALUOut := MuxCase(
        io.EXPipReg.alu,
        Seq(
        (io.EXPipReg.MEM.MemToReg === WB_MEM) -> io.DcacheData,
        (io.EXPipReg.MEM.MemToReg === WB_PC4) -> (io.EXPipReg.pc + 4.U),
        )
    )

    io.WBPipReg.data := io.MemALUOut

    // debug
    io.WBPipReg.pc          := io.EXPipReg.pc
    io.WBPipReg.inst        := io.EXPipReg.inst
    io.WBPipReg.commit      := io.EXPipReg.commit
}