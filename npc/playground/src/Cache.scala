import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

import AXI._

case class CacheConfig(nWays: Int, nSets: Int, blockBytes: Int)

object CacheState extends ChiselEnum {
  val sIdle, sRead, sRefill, sRefillReady = Value
}

class ICacheReq(addrWidth: Int, dataWidth: Int) extends Bundle {
  val addr = UInt(addrWidth.W)
}

class ICacheResp(dataWidth: Int) extends Bundle {
  val data = UInt(dataWidth.W)
}

class ICacheIO(addrWidth: Int, dataWidth: Int) extends Bundle {
  val req  = Flipped(Valid(new ICacheReq(addrWidth, dataWidth)))
  val resp = Valid(new ICacheResp(dataWidth))
}

class ICacheModuleIO(AxiParams: AxiBundleParameters, addrWidth: Int, dataWidth: Int) extends Bundle {
  val cpu = new ICacheIO(addrWidth, dataWidth)
  val axi = new AxiBundle(AxiParams)
}

class ICache(val p: CacheConfig, val axi: AxiBundleParameters, val xlen: Int) extends Module {
  val nWays          = p.nWays
  val nSets          = p.nSets
  val bBytes         = p.blockBytes
  val wBytes         = xlen / 8
  val bBits          = bBytes << 3
  val nWords         = bBits / xlen
  val dataBeats      = bBits / axi.dataBits * 2
  val byteOffsetBits = log2Ceil(wBytes)

  val blen = log2Ceil(bBytes)
  val slen = log2Ceil(nSets)
  val tlen = xlen - (slen + blen)

  val io = IO(new ICacheModuleIO(axi, addrWidth = xlen, dataWidth = xlen))

  val hit = Wire(Bool())

  val v = RegInit(0.U(nSets.W))
  val refill_buf = Reg(Vec(dataBeats, UInt(axi.addrBits.W)))
  val metaMem = SyncReadMem(nSets, UInt(tlen.W))
  val dataMem = Seq.fill(nWords)(SyncReadMem(nSets, Vec(wBytes, UInt(8.W))))

  

  val addr_reg = Reg(chiselTypeOf(io.cpu.req.bits.addr))

  val addr = io.cpu.req.bits.addr
  val idx = addr(slen + blen - 1, blen)
  val tag_reg = addr_reg(xlen - 1, slen + blen)
  val idx_reg = addr_reg(slen + blen - 1, blen)
  val off_reg = addr_reg(blen - 1, byteOffsetBits)

  val rmeta = metaMem.read(idx)

  val (read_count, read_wrap_out) = Counter(io.axi.r.fire, dataBeats)

  import CacheState._
  val state = RegInit(sIdle)

  val is_idle        = state === sIdle
  val is_read        = state === sRead
  val is_refill      = state === sRefill
  val is_refillReady = state === sRefillReady
  val is_alloc       = state === sRefill && read_wrap_out 
  val is_alloc_reg   = RegNext(is_alloc)

  hit := v(idx_reg) && rmeta === tag_reg

  when(io.cpu.resp.valid) {
    addr_reg := addr
  }

  io.axi.aw.bits := AxiAddressBundle(axi)(
    0.U,
    (Cat(tag_reg, idx_reg) << blen.U).asUInt,
    log2Up(axi.dataBits / 8).U - 1.U,
    (dataBeats - 1).U
  )

  io.axi.ar.bits := AxiAddressBundle(axi)(
    0.U,
    (Cat(tag_reg, idx_reg) << blen.U).asUInt,
    log2Up(axi.dataBits / 8).U - 1.U,
    (dataBeats - 1).U
  )

  io.axi.w.bits := AxiWriteDataBundle(axi)(
    0.U,
    Some(Fill(wBytes, 1.U)),
    false.B
  )

  val read = dontTouch(refill_buf.asUInt)

  io.cpu.resp.valid := is_read && hit || is_idle
  io.cpu.resp.bits.data := VecInit.tabulate(nWords)(i => read((i + 1) * xlen - 1, i * xlen))(off_reg)

  io.axi.r.ready := is_refill


  io.axi.ar.valid := false.B
  io.axi.aw.valid := false.B
  io.axi.w.valid := false.B
  io.axi.b.ready := false.B
  
  when(io.axi.r.fire) {
    refill_buf(read_count) := io.axi.r.bits.data(31, 0)
  }

  when(is_alloc_reg) {
    v := v.bitSet(idx_reg, true.B)
    metaMem.write(idx_reg, tag_reg)
    dataMem.zipWithIndex.foreach {
      case (mem, i) =>
        val data = VecInit.tabulate(wBytes)(j => refill_buf(i)((j + 1) * 8 - 1, j * 8))
        mem.write(idx_reg, data)
        mem.suggestName(s"dataMem_${i}")
    }
  }


  switch(state) {
    is(sIdle) {
      when(io.cpu.req.valid) {
        state := sRead
      }
    }
    is(sRead) {
      when(hit) {
        when(io.cpu.req.valid) {
          state := sRead
        }.otherwise {
          state := sIdle
        }
      }.otherwise {
        state := sRefillReady
      }
    }
    is(sRefill) {
      when(read_wrap_out) {
        state := sIdle
      }
    }
    is(sRefillReady) {
      io.axi.ar.valid := true.B
      when(io.axi.ar.fire) {
        state := sRefill
      }
    }
  }
}

class DCACHEIO(xlen: Int) extends Bundle {
  val addr  = Input(UInt(xlen.W))
  val data  = Output(UInt(xlen.W))
  val wen   = Input(Bool())
  val wdata = Input(UInt(xlen.W))
  val mask  = Input(UInt(4.W))
}
