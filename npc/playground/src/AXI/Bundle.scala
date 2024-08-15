package AXI

import chisel3._
import chisel3.util._

object AxiConstants {
  // These are all fixed by the standard:
  val LenBits   = 8
  val SizeBits  = 3
  val BurstBits = 2
  val LockBits  = 1
  val CacheBits = 4
  val ProtBits  = 3
  val QosBits   = 4
  val RespBits  = 2

  def CacheReadAllocate  = 8.U(CacheBits.W)
  def CacheWriteAllocate = 4.U(CacheBits.W)
  def CacheModifiable    = 2.U(CacheBits.W)
  def CacheBufferable    = 1.U(CacheBits.W)

  def ProtPrivileged  = 1.U(ProtBits.W)
  def ProtInsecure    = 2.U(ProtBits.W)
  def ProtInstruction = 4.U(ProtBits.W)

  def BurstFixed = 0.U(BurstBits.W)
  def BurstIncr  = 1.U(BurstBits.W)
  def BurstWrap  = 2.U(BurstBits.W)

  def RespOkay   = 0.U(RespBits.W)
  def RespExOkay = 1.U(RespBits.W)
  def RespSlvErr = 2.U(RespBits.W)
  def RespDevErr = 3.U(RespBits.W)
}

case class AxiBundleParameters(
  addrBits: Int,
  dataBits: Int,
  idBits:   Int) {
  require(dataBits >= 8, s"AXI4 data bits must be >= 8 (got $dataBits)")
  require(addrBits >= 1, s"AXI4 addr bits must be >= 1 (got $addrBits)")
  require(idBits >= 1, s"AXI4 id bits must be >= 1 (got $idBits)")
  require(isPow2(dataBits), s"AXI4 data bits must be pow2 (got $dataBits)")
}

/** aka the AW/AR channel */
class AxiAddressBundle(params: AxiBundleParameters) extends Bundle {
  val id    = UInt(params.idBits.W)
  val addr  = UInt(params.addrBits.W)
  val len   = UInt(AxiConstants.LenBits.W) // number of beats - 1
  val size  = UInt(AxiConstants.SizeBits.W) // bytes in beat = 2^size
  val burst = UInt(AxiConstants.BurstBits.W)
}

object AxiAddressBundle {
  def apply(params: AxiBundleParameters)(id: UInt, addr: UInt, size: UInt, len: UInt = 0.U): AxiAddressBundle = {
    val aw = Wire(new AxiAddressBundle(params))
    aw.id    := id
    aw.addr  := addr
    aw.len   := len
    aw.size  := size
    aw.burst := AxiConstants.BurstIncr
    aw
  }
}

/** aka the W-channel */
class AxiWriteDataBundle(params: AxiBundleParameters) extends Bundle {
  // id removed
  val data = UInt(params.dataBits.W)
  val strb = UInt((params.dataBits / 8).W)
  val last = Bool()
}

object AxiWriteDataBundle {
  def apply(
    params: AxiBundleParameters
  )(data:   UInt,
    strb:   Option[UInt] = None,
    last:   Bool         = true.B
  ): AxiWriteDataBundle = {
    val w = Wire(new AxiWriteDataBundle(params))
    w.strb := strb.getOrElse(Fill(params.dataBits / 8, 1.U))
    w.data := data
    w.last := last
    w
  }
}

/** aka the R-channel */
class AxiReadDataBundle(params: AxiBundleParameters) extends Bundle {
  val id   = UInt(params.idBits.W)
  val data = UInt(params.dataBits.W)
  val resp = UInt(AxiConstants.RespBits.W)
  val last = Bool()
}

object AxiReadDataBundle {
  def apply(
    params: AxiBundleParameters
  )(id:     UInt,
    data:   UInt,
    last:   Bool,
    resp:   UInt = 0.U
  ): AxiReadDataBundle = {
    val r = Wire(new AxiReadDataBundle(params))
    r.id   := id
    r.data := data
    r.last := last
    r.resp := resp
    r
  }
}

/** aka the B-channel */
class AxiWriteResponseBundle(params: AxiBundleParameters) extends Bundle {
  val id   = UInt(params.idBits.W)
  val resp = UInt(AxiConstants.RespBits.W)
}

object AxiWriteResponseBundle {
  def apply(params: AxiBundleParameters)(id: UInt, resp: UInt = 0.U): AxiWriteResponseBundle = {
    val b = Wire(new AxiWriteResponseBundle(params))
    b.id   := id
    b.resp := resp
    b
  }
}

class AxiBundle(params: AxiBundleParameters) extends Bundle {
  val aw = Irrevocable(new AxiAddressBundle(params))
  val w  = Irrevocable(new AxiWriteDataBundle(params))
  val b  = Flipped(Irrevocable(new AxiWriteResponseBundle(params)))
  val ar = Irrevocable(new AxiAddressBundle(params))
  val r  = Flipped(Irrevocable(new AxiReadDataBundle(params)))
}

object AxiBunle {
  def apply(params: AxiBundleParameters): AxiBundle = new AxiBundle(params)
}
