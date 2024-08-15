import chisel3._
import chisel3.util._
import AXI._

object Tile {
  def apply(config: Config): Tile = new Tile(config.core, config.axi, config.cache)
}

class TileIO(xlen: Int, axiParams: AxiBundleParameters) extends Bundle {
  val interrupt = Input(Bool())
  val master = new AxiBundle(axiParams)
  val slave = Flipped(new AxiBundle(axiParams))
}

class Tile(val coreParams: CoreConfig, val axiParams: AxiBundleParameters, val cacheParams: CacheConfig) extends Module {
  val io = IO(new TileIO(coreParams.xlen, axiParams))
  val core = Module(new Core(coreParams, axiParams))
  val icache = Module(new ICache(cacheParams, axiParams, coreParams.xlen))

  io.slave := DontCare

  core.io.icache <> icache.io.cpu
  io.master <> icache.io.axi

}
