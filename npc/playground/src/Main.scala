import circt.stage._
import chisel3.stage.ChiselGeneratorAnnotation

object Elaborate extends App {
  val config = MiniConfig()

  def top       = new Tile(config.core, config.axi, config.cache)
  val generator = Seq(chisel3.stage.ChiselGeneratorAnnotation(() => top))

  val splitVerilogArgs = args ++ Array("--split-verilog")

  (new ChiselStage).execute(splitVerilogArgs, generator :+ CIRCTTargetAnnotation(CIRCTTarget.Verilog))
}
