import chisel3._
import chiseltest._

import org.scalatest.flatspec.AnyFlatSpec
import chiseltest.ChiselScalatestTester

class ICacheTester(icache: => ICache) extends Module {
    val dut = Module(icache)

}

class ICacheTests extends AnyFlatSpec with ChiselScalatestTester {
    val p = MiniConfig()

    "Cache" should "pass" in {
        test(new ICacheTester(new ICache(p.cache, p.axi, p.axi.addrBits))) { dut =>
            dut.dut.io.cpu.req.bits.addr.poke(0x20000000.U)
            dut.dut.io.cpu.req.valid.poke(true.B)
            dut.clock.step()
            print("shit: " + dut.dut.state.peek())
        }
    }
}