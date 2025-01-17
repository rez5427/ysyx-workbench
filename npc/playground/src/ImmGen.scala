import chisel3._
import chisel3.util._
import Control._

class ImmGenIO(xlen: Int) extends Bundle {
  val inst = Input(UInt(xlen.W))
  val sel  = Input(UInt(3.W))
  val out  = Output(UInt(xlen.W))
}

trait ImmGen extends Module {
  def xlen: Int
  val io: ImmGenIO
}

class ImmGenWire(val xlen: Int) extends ImmGen {
  val io   = IO(new ImmGenIO(xlen))
  val Iimm = io.inst(31, 20).asSInt
  val Simm = Cat(io.inst(31, 25), io.inst(11, 7)).asSInt
  val Bimm = Cat(io.inst(31), io.inst(7), io.inst(30, 25), io.inst(11, 8), 0.U(1.W)).asSInt
  val Uimm = Cat(io.inst(31, 12), 0.U(12.W)).asSInt
  val Jimm = Cat(io.inst(31), io.inst(19, 12), io.inst(20), io.inst(30, 25), io.inst(24, 21), 0.U(1.W)).asSInt
  val Zimm = io.inst(19, 15).zext

  io.out := MuxLookup(
    io.sel,
    Iimm & (-2).S,
    Seq(Ext_immI -> Iimm, Ext_immS -> Simm, Ext_immB -> Bimm, Ext_immU -> Uimm, Ext_immJ -> Jimm, Ext_immZ -> Zimm)
  ).asUInt
}
