import chisel3._
import AXI._

case class Config(core: CoreConfig, cache: CacheConfig, axi: AxiBundleParameters)

object MiniConfig {
  def apply(): Config = {
    val xlen = 32
    Config(
      core = CoreConfig(
        xlen       = xlen
      ),
      cache = CacheConfig(
        nWays      = 1,
        nSets      = 256,
        blockBytes = 4 * (xlen / 8)
      ),
      axi = AxiBundleParameters(
        addrBits = 32,
        dataBits = 64,
        idBits   = 4
      )
    )
  }
}
