import chisel3._
import chisel3.util._ 
import chisel3.experimental.{ChiselAnnotation, annotate}
import chisel3.util.experimental.BoringUtils

class RegFileIO(xlen: Int) extends Bundle {
    val Ra    = Input(UInt(5.W))
    val Rb    = Input(UInt(5.W))
    val Rw    = Input(UInt(5.W))
    val busW  = Input(UInt(xlen.W))
    val RegWr = Input(Bool())
    val busA  = Output(UInt(xlen.W))
    val busB  = Output(UInt(xlen.W))

    // debug signal
    val regs = Output(Vec(32, UInt(xlen.W)))
}

class RegFile(xlen: Int) extends Module {
    val io   = IO(new RegFileIO(xlen))
    val regs = Mem(32, UInt(xlen.W))

    io.busA := Mux(io.Ra.orR, regs(io.Ra), 0.U)
    io.busB := Mux(io.Rb.orR, regs(io.Rb), 0.U)

    // Connect the output vector to the internal register memory
    io.regs := VecInit(Seq.tabulate(32)(i => regs(i.U)))

    when(io.RegWr & io.Rw.orR) {
        regs(io.Rw) := io.busW
    }
}
