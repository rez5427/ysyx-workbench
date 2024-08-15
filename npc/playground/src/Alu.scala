import chisel3._
import chisel3.util._

object Alu {
  val ALU_ADD    = 0.U(4.W)
  val ALU_SUB    = 1.U(4.W)
  val ALU_AND    = 2.U(4.W)
  val ALU_OR     = 3.U(4.W)
  val ALU_XOR    = 4.U(4.W)
  val ALU_SLT    = 5.U(4.W)
  val ALU_SLL    = 6.U(4.W)
  val ALU_SLTU   = 7.U(4.W)
  val ALU_SRL    = 8.U(4.W)
  val ALU_SRA    = 9.U(4.W)
  val ALU_COPY_A = 10.U(4.W)
  val ALU_COPY_B = 11.U(4.W)
  val ALU_XXX    = 15.U(4.W)
}

class AluIO(width: Int) extends Bundle {
  val A      = Input(UInt(width.W))
  val B      = Input(UInt(width.W))
  val alu_op = Input(UInt(4.W))
  val out    = Output(UInt(width.W))
}

import Alu._

trait Alu extends Module {
  def width: Int
  val io: AluIO
}

class Alu(val width: Int) extends Alu {
  val io = IO(new AluIO(width))

  val shamt = io.B(4, 0).asUInt

  io.out := MuxLookup(
    io.alu_op,
    io.B,
    (
      Seq(
        ALU_ADD -> (io.A + io.B),
        ALU_SUB -> (io.A - io.B),
        ALU_SRA -> (io.A.asSInt >> shamt).asUInt,
        ALU_SRL -> (io.A >> shamt),
        ALU_SLL -> (io.A << shamt),
        ALU_SLT -> (io.A.asSInt < io.B.asSInt),
        ALU_SLTU -> (io.A < io.B),
        ALU_AND -> (io.A & io.B),
        ALU_OR -> (io.A | io.B),
        ALU_XOR -> (io.A ^ io.B),
        ALU_COPY_A -> io.A
      )
    )
  )
}