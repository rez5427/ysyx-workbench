class IFIO(xlen: Int) extends Bundle {
    val icache = new ICacheIO(xlen, xlen * 2)

    val exe_alu_out = Input(UInt(xlen.W))

    val ifid_inst = Output(UInt(xlen.W))
    val ifid_pc = Output(UInt(xlen.W))
}

class IF(xlen: Int) extends Bundle {
    val io         = IO(new IFIO(xlen))

    val stall_flg   = !io.icache.resp.valid

    val started = RegNext(reset.asBool)
    val pc      = RegInit(Const.PC_START.U - 4.U)

    val if_inst = Mux(started || io.ctrl.kill || brCond.io.taken, Instructions.NOP, io.icache.resp.bits.data)
  

    val pc_next = MuxCase(
        pc + 4.U,
        IndexedSeq(
        ((brCond.io.taken) || (idex_reg.MEM_Reg.PC_SEL === PC_ALU)) -> (alu.io.out >> 1.U << 1.U),
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
}