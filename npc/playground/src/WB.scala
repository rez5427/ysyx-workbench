import chisel3._
import chisel3.util._
import chisel3.experimental.{annotate, ChiselAnnotation}
import chisel3.util.experimental.BoringUtils

import Control._

class WBIO(xlen: Int) extends Bundle {
    val WBPipReg    = Flipped((new MEMWBPipelineRegister(xlen)))

    val busw        = Output(UInt(xlen.W))
    val Rd          = Output(UInt(5.W))
    val RegWr       = Output(UInt(1.W))

    // debug signal
    def generateRegNames(prefix: String, count: Int): Seq[String] = {
        (0 until count).map(i => s"${prefix}_${i}")
    }
    val regNames = generateRegNames("regs", 32)
    val regs = Input(Vec(32, UInt(xlen.W)))
}

class WB(val conf: CoreConfig) extends BlackBox with HasBlackBoxInline {
  val io = IO(new WBIO(conf.xlen))

  // Generate the list of register names for Verilog
  val regNamesVerilog = io.regNames.zipWithIndex.map { case (name, idx) =>
    s"\tinput [${conf.xlen-1}:0] ${name}"
  }.mkString(",\n")

  setInline("WB.sv",
      s"""module WB(
          |${regNamesVerilog},  // Debug signals
          |  input [${conf.xlen-1}:0] WBPipReg_data,
          |  input [31:0] WBPipReg_pc,
          |  input [31:0] WBPipReg_inst,
          |  input WBPipReg_commit,
          |  input WBPipReg_WB_RegWr,
          |  input [4:0] WBPipReg_WB_Rd,
          |  output [${conf.xlen-1}:0] busw,
          |  output [4:0] Rd,
          |  output RegWr
          |);
          |
          |// Declare a 32-element int array for the DPI-C function call
          |int regs_array [31:0];
          |
          |// Assign each input to the array
          |${(0 until 32).map(i => s"assign regs_array[$i] = regs_$i;").mkString("\n")}
          |
          |// DPI-C function declaration outside of the port list
          |import "DPI-C" function void trigger_nemu_difftest(input int commit, input int regs[32], input int pc, input int inst, input int busw, input int Rd, input int RegWr);
          |
          |// Assign the inputs to outputs directly or use logic to assign them
          |assign busw = WBPipReg_data;   // Example of direct assignment
          |assign Rd = WBPipReg_WB_Rd;    // Example of direct assignment
          |assign RegWr = WBPipReg_WB_RegWr;  // Example of direct assignment
          |
          |// Widen the commit signal to 32 bits before passing to DPI-C function
          |wire [31:0] WBPipReg_commit_wide = {31'b0, WBPipReg_commit};
          |
          |wire [31:0] Rd_wide = {27'b0, Rd};
          |wire [31:0] RegWr_wide = {31'b0, RegWr};
          |
          |always @(*) begin
          |    trigger_nemu_difftest(WBPipReg_commit_wide, regs_array, WBPipReg_pc, WBPipReg_inst, busw, Rd_wide, RegWr_wide);
          |end
          |endmodule
      """.stripMargin)
}
