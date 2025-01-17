import chisel3._
import chisel3.util._

import Control._

object Const {
  val PC_START = 0x20000000
}

class IFIO(xlen: Int) extends Bundle {
    val icache = Flipped(new ICacheIO(xlen, xlen * 2))

    val exe_alu_out = Input(UInt(xlen.W))

    val taken = Input(Bool())

    val idex_pc_sel = Input(UInt(2.W))

    val if_stall = Output(Bool())

    val IFPipReg = (new IFIDPipelineRegister(xlen))
}

class IFU(val conf: CoreConfig) extends Module {
    val io         = IO(new IFIO(conf.xlen))

    val started = RegNext(reset.asBool)
    val pc = RegInit(Const.PC_START.U(conf.xlen.W) - 4.U(conf.xlen.W))
    //val inst = RegInit(Instructions.NOP.U(conf.xlen.W))

    val isJump = ((io.icache.resp.bits.data(6, 0) === Instructions.JALR(6, 0)) || (io.icache.resp.bits.data(6, 0) === Instructions.JAL(6, 0)))

    val stall = !io.icache.resp.valid || reset.asBool

    val if_inst = Mux(started || io.taken || stall, Instructions.NOP, io.icache.resp.bits.data)

    val pc_next = MuxCase(
        pc + 4.U,
        IndexedSeq(
        ((io.taken) || (io.idex_pc_sel === PC_ALU)) -> (io.exe_alu_out >> 1.U << 1.U),
        (stall || isJump.asBool )-> pc
        )
    )

    /*
        Pipline Handshake

                -----------             -----------   
                |         |    valid    |         |   
                |         |-----------> |         |
                |   I F   |             |   I D   |
                |         |    ready    |         |  
                |         |<----------- |         |
                -----------             -----------
    

    val if_valid = RegInit(0.U(1.W))
    val if_ready_go = io.icache.resp.valid
    val if_allowin = !if_valid || if_ready_go && io.IFPipReg.ready
    io.IFPipReg.valid := if_valid & if_ready_go

    when(if_allowin) {
        if_valid := ~reset.asBool
    }

    when(reset.asBool) {
        pc := Const.PC_START.U
    }

    when(if_allowin && !reset.asBool) {
        pc := pc_next
    }

    
        Pipline Handshake finished
    */

    pc := pc_next
    io.icache.req.bits.addr := pc_next
    io.icache.req.valid := !stall

    dontTouch(isJump)

    io.if_stall := stall

    io.IFPipReg.pc := pc
    io.IFPipReg.inst := if_inst
    io.IFPipReg.commit := io.icache.req.valid && io.icache.resp.valid && !reset.asBool
}