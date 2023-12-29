import chisel3._
import Control._

class BrCondIO(xlen: Int) extends Bundle {
  val rs1 = Input(UInt(xlen.W))
  val rs2 = Input(UInt(xlen.W))
  val br_type = Input(UInt(3.W))
  val taken = Output(Bool())
}

trait BrCond extends Module {
  def xlen: Int
  val io: BrCondIO

  val diff = io.rs1 - io.rs2
  val neq = diff.orR
  val eq = !neq
  val isSameSign = io.rs1(xlen - 1) === io.rs2(xlen - 1)
  val lt = Mux(isSameSign, diff(xlen - 1), io.rs1(xlen - 1))
  val ltu = Mux(isSameSign, diff(xlen - 1), io.rs2(xlen - 1))
  val ge = !lt
  val geu = !ltu
  io.taken :=
    ((io.br_type === Br_Eq) && eq) ||
      ((io.br_type === Br_Neq) && neq) ||
      ((io.br_type === Br_Lt) && lt) ||
      ((io.br_type === Br_Ge) && ge) ||
      ((io.br_type === Br_Ltu) && ltu) ||
      ((io.br_type === Br_Geu) && geu)
}