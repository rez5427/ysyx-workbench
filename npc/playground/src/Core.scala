import chisel3._
import chisel3.util._
import AXI._

case class CoreConfig(
  xlen:       Int,
  makeAlu:    Int => Alu    = new AluSimple(_),
  makeBrCond: Int => BrCond = new BrCondSimple(_),
  makeImmGen: Int => ImmGen = new ImmGenWire(_))

class CoreIO(xlen: Int, axiParams: AxiBundleParameters) extends Bundle {
  val icache = Flipped(new ICacheIO(xlen, xlen))
  //val dcache = Flipped(new CacheIO(xlen, xlen))
}

class Core(val coreConf: CoreConfig, val axiParams: AxiBundleParameters) extends Module {
  val io    = IO(new CoreIO(coreConf.xlen, axiParams))
  val dpath = Module(new DataPath(coreConf))

  dpath.io.dcache := DontCare

  dpath.io.icache <> io.icache
}
