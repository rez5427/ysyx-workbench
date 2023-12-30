import chisel3._
import chisel3.util.Valid

case class CoreConfig(
  xlen:       Int,
  makeAlu:    Int => Alu = new AluSimple(_),
  makeBrCond: Int => BrCond = new BrCondSimple(_),
  makeImmGen: Int => ImmGen = new ImmGenWire(_))

class CoreIO(xlen: Int) extends Bundle {
  val icache = Flipped(new ICACHEIO(xlen))
  val dcache = Flipped(new DCACHEIO(xlen))
}

class Core(val conf: CoreConfig) extends Module {
  val io = IO(new CoreIO(conf.xlen))
  val dpath = Module(new DataPath(conf))
  val ctrl = Module(new Control)

  dpath.io.icache <> io.icache
  dpath.io.dcache <> io.dcache
  dpath.io.ctrl <> ctrl.io
}