import chisel3._
import chisel3.util._

object Control {
  // pc_sel
  val PC_4   = 0.U(2.W)
  val PC_ALU = 1.U(2.W)

  // ExtOP
  val Ext_XXXX = 0.U(2.W)
  val Ext_immI = 1.U(3.W)
  val Ext_immU = 2.U(3.W)
  val Ext_immS = 3.U(3.W)
  val Ext_immB = 4.U(3.W)
  val Ext_immJ = 5.U(3.W)
  val Ext_immZ = 6.U(3.W)

  // RegWr
  val RegWr_N = 0.U(1.W)
  val RegWr_Y = 1.U(1.W)

  // ALUASrc
  val A_XXX = 0.U(1.W)
  val A_RS1 = 0.U(1.W)
  val A_PC  = 1.U(1.W)

  // ALUBSrc
  val B_XXX = 0.U(1.W)
  val B_RS2 = 0.U(1.W)
  val B_Imm = 1.U(1.W)

  // Branch
  val Br_XXX = 0.U(3.W)
  val Br_Eq  = 1.U(3.W)
  val Br_Neq = 2.U(3.W)
  val Br_Lt  = 3.U(3.W)
  val Br_Ge  = 4.U(3.W)
  val Br_Ltu = 5.U(3.W)
  val Br_Geu = 6.U(3.W)

  // wb_sel
  val WB_XXX = 0.U(2.W)
  val WB_ALU = 0.U(2.W)
  val WB_MEM = 1.U(2.W)
  val WB_PC4 = 2.U(2.W)

  // Wen
  val Wen_XXX = 0.U(1.W)
  val Wen_Y   = 1.U(1.W)
  val Wen_N   = 0.U(1.W)

  // mask
  val Mask_XXX = 0.U(4.W)
  val Mask_B   = 1.U(4.W)
  val Mask_H   = 3.U(4.W)
  val Mask_W   = 15.U(4.W)

  // kill
  val Kill_N = 0.U(1.W)
  val Kill_Y = 1.U(1.W)

  import Alu._
  import Instructions._

  val default =
    //
    //                  pc_sel  A_sel   B_sel   imm_sel     alu_op      br_type   wb_sel     Reg_Wr     mask      Wen      kill
    //                     |      |       |        |           |          |         |          |          |        |        |
              List(     PC_4,   A_XXX,  B_XXX, Ext_XXXX,    ALU_XXX,    Br_XXX,   WB_XXX,   RegWr_N,  Mask_XXX, Wen_XXX,  Kill_N)
  val map = Array(
    LUI ->    List(     PC_4,   A_PC,   B_Imm, Ext_immU,    ALU_COPY_B, Br_XXX,   WB_ALU,   RegWr_Y,  Mask_XXX, Wen_XXX,  Kill_N),
    AUIPC ->  List(     PC_4,   A_PC,   B_Imm, Ext_immU,    ALU_ADD,    Br_XXX,   WB_ALU,   RegWr_Y,  Mask_XXX, Wen_XXX,  Kill_N),
    JAL ->    List(     PC_ALU, A_PC,   B_Imm, Ext_immJ,    ALU_ADD,    Br_XXX,   WB_PC4,   RegWr_Y,  Mask_XXX, Wen_XXX,  Kill_Y),
    JALR ->   List(     PC_ALU, A_RS1,  B_Imm, Ext_immI,    ALU_ADD,    Br_XXX,   WB_PC4,   RegWr_Y,  Mask_XXX, Wen_XXX,  Kill_Y),
    BEQ ->    List(     PC_4,   A_PC,   B_Imm, Ext_immB,    ALU_ADD,    Br_Eq,    WB_ALU,   RegWr_N,  Mask_XXX, Wen_XXX,  Kill_N),
    BNE ->    List(     PC_4,   A_PC,   B_Imm, Ext_immB,    ALU_ADD,    Br_Neq,   WB_ALU,   RegWr_N,  Mask_XXX, Wen_XXX,  Kill_N),
    BLT ->    List(     PC_4,   A_PC,   B_Imm, Ext_immB,    ALU_ADD,    Br_Lt,    WB_ALU,   RegWr_N,  Mask_XXX, Wen_XXX,  Kill_N),
    BGE ->    List(     PC_4,   A_PC,   B_Imm, Ext_immB,    ALU_ADD,    Br_Ge,    WB_ALU,   RegWr_N,  Mask_XXX, Wen_XXX,  Kill_N),
    BLTU ->   List(     PC_4,   A_PC,   B_Imm, Ext_immB,    ALU_ADD,    Br_Ltu,   WB_ALU,   RegWr_N,  Mask_XXX, Wen_XXX,  Kill_N),
    BGEU ->   List(     PC_4,   A_PC,   B_Imm, Ext_immB,    ALU_ADD,    Br_Geu,   WB_ALU,   RegWr_N,  Mask_XXX, Wen_XXX,  Kill_N),
    LB ->     List(     PC_4,   A_RS1,  B_Imm, Ext_immI,    ALU_ADD,    Br_XXX,   WB_MEM,   RegWr_Y,  Mask_B,   Wen_N,    Kill_Y),
    LH ->     List(     PC_4,   A_RS1,  B_Imm, Ext_immI,    ALU_ADD,    Br_XXX,   WB_MEM,   RegWr_Y,  Mask_H,   Wen_N,    Kill_Y),
    LW ->     List(     PC_4,   A_RS1,  B_Imm, Ext_immI,    ALU_ADD,    Br_XXX,   WB_MEM,   RegWr_Y,  Mask_W,   Wen_N,    Kill_Y),
    LBU ->    List(     PC_4,   A_RS1,  B_Imm, Ext_immI,    ALU_ADD,    Br_XXX,   WB_MEM,   RegWr_Y,  Mask_XXX, Wen_N,    Kill_Y),
    LHU ->    List(     PC_4,   A_RS1,  B_Imm, Ext_immI,    ALU_ADD,    Br_XXX,   WB_MEM,   RegWr_Y,  Mask_XXX, Wen_N,    Kill_Y),
    SB ->     List(     PC_4,   A_RS1,  B_Imm, Ext_immS,    ALU_ADD,    Br_XXX,   WB_XXX,   RegWr_N,  Mask_B,   Wen_Y,    Kill_N),
    SH ->     List(     PC_4,   A_RS1,  B_Imm, Ext_immS,    ALU_ADD,    Br_XXX,   WB_XXX,   RegWr_N,  Mask_H,   Wen_Y,    Kill_N),
    SW ->     List(     PC_4,   A_RS1,  B_Imm, Ext_immS,    ALU_ADD,    Br_XXX,   WB_XXX,   RegWr_N,  Mask_W,   Wen_Y,    Kill_N),
    ADDI ->   List(     PC_4,   A_RS1,  B_Imm, Ext_immI,    ALU_ADD,    Br_XXX,   WB_ALU,   RegWr_Y,  Mask_XXX, Wen_XXX,  Kill_N),
    SLTI ->   List(     PC_4,   A_RS1,  B_Imm, Ext_immI,    ALU_SLT,    Br_XXX,   WB_ALU,   RegWr_Y,  Mask_XXX, Wen_XXX,  Kill_N),
    SLTIU ->  List(     PC_4,   A_RS1,  B_Imm, Ext_immI,    ALU_SLTU,   Br_XXX,   WB_ALU,   RegWr_Y,  Mask_XXX, Wen_XXX,  Kill_N),
    XORI ->   List(     PC_4,   A_RS1,  B_Imm, Ext_immI,    ALU_XOR,    Br_XXX,   WB_ALU,   RegWr_Y,  Mask_XXX, Wen_XXX,  Kill_N),
    ORI ->    List(     PC_4,   A_RS1,  B_Imm, Ext_immI,    ALU_OR,     Br_XXX,   WB_ALU,   RegWr_Y,  Mask_XXX, Wen_XXX,  Kill_N),
    ANDI ->   List(     PC_4,   A_RS1,  B_Imm, Ext_immI,    ALU_AND,    Br_XXX,   WB_ALU,   RegWr_Y,  Mask_XXX, Wen_XXX,  Kill_N),
    SLLI ->   List(     PC_4,   A_RS1,  B_Imm, Ext_immI,    ALU_SLL,    Br_XXX,   WB_ALU,   RegWr_Y,  Mask_XXX, Wen_XXX,  Kill_N),
    SRLI ->   List(     PC_4,   A_RS1,  B_Imm, Ext_immI,    ALU_SRL,    Br_XXX,   WB_ALU,   RegWr_Y,  Mask_XXX, Wen_XXX,  Kill_N),
    SRAI ->   List(     PC_4,   A_RS1,  B_Imm, Ext_immI,    ALU_SRA,    Br_XXX,   WB_ALU,   RegWr_Y,  Mask_XXX, Wen_XXX,  Kill_N),
    ADD ->    List(     PC_4,   A_RS1,  B_RS2, Ext_XXXX,    ALU_ADD,    Br_XXX,   WB_ALU,   RegWr_Y,  Mask_XXX, Wen_XXX,  Kill_N),
    SUB ->    List(     PC_4,   A_RS1,  B_RS2, Ext_XXXX,    ALU_SUB,    Br_XXX,   WB_ALU,   RegWr_Y,  Mask_XXX, Wen_XXX,  Kill_N),
    SLL ->    List(     PC_4,   A_RS1,  B_RS2, Ext_XXXX,    ALU_SLL,    Br_XXX,   WB_ALU,   RegWr_Y,  Mask_XXX, Wen_XXX,  Kill_N),
    SLT ->    List(     PC_4,   A_RS1,  B_RS2, Ext_XXXX,    ALU_SLT,    Br_XXX,   WB_ALU,   RegWr_Y,  Mask_XXX, Wen_XXX,  Kill_N),
    SLTU ->   List(     PC_4,   A_RS1,  B_RS2, Ext_XXXX,    ALU_SLTU,   Br_XXX,   WB_ALU,   RegWr_Y,  Mask_XXX, Wen_XXX,  Kill_N),
    XOR ->    List(     PC_4,   A_RS1,  B_RS2, Ext_XXXX,    ALU_XOR,    Br_XXX,   WB_ALU,   RegWr_Y,  Mask_XXX, Wen_XXX,  Kill_N),
    SRL ->    List(     PC_4,   A_RS1,  B_RS2, Ext_XXXX,    ALU_SRL,    Br_XXX,   WB_ALU,   RegWr_Y,  Mask_XXX, Wen_XXX,  Kill_N),
    SRA ->    List(     PC_4,   A_RS1,  B_RS2, Ext_XXXX,    ALU_SRA,    Br_XXX,   WB_ALU,   RegWr_Y,  Mask_XXX, Wen_XXX,  Kill_N),
    OR ->     List(     PC_4,   A_RS1,  B_RS2, Ext_XXXX,    ALU_OR,     Br_XXX,   WB_ALU,   RegWr_Y,  Mask_XXX, Wen_XXX,  Kill_N),
    AND ->    List(     PC_4,   A_RS1,  B_RS2, Ext_XXXX,    ALU_AND,    Br_XXX,   WB_ALU,   RegWr_Y,  Mask_XXX, Wen_XXX,  Kill_N),
    FENCE ->  List(     PC_4,   A_XXX,  B_XXX, Ext_XXXX,    ALU_XXX,    Br_XXX,   WB_XXX,   RegWr_N,  Mask_XXX, Wen_XXX,  Kill_N),
    FENCEI -> List(     PC_4,   A_XXX,  B_XXX, Ext_XXXX,    ALU_XXX,    Br_XXX,   WB_XXX,   RegWr_N,  Mask_XXX, Wen_XXX,  Kill_N),
    ECALL ->  List(     PC_4,   A_XXX,  B_XXX, Ext_XXXX,    ALU_XXX,    Br_XXX,   WB_XXX,   RegWr_N,  Mask_XXX, Wen_XXX,  Kill_N),
    CSRRW ->  List(     PC_4,   A_XXX,  B_XXX, Ext_XXXX,    ALU_XXX,    Br_XXX,   WB_XXX,   RegWr_N,  Mask_XXX, Wen_XXX,  Kill_N),
    CSRRS ->  List(     PC_4,   A_XXX,  B_XXX, Ext_XXXX,    ALU_XXX,    Br_XXX,   WB_XXX,   RegWr_N,  Mask_XXX, Wen_XXX,  Kill_N),
    CSRRC ->  List(     PC_4,   A_XXX,  B_XXX, Ext_XXXX,    ALU_XXX,    Br_XXX,   WB_XXX,   RegWr_N,  Mask_XXX, Wen_XXX,  Kill_N),
    CSRRWI -> List(     PC_4,   A_XXX,  B_XXX, Ext_XXXX,    ALU_XXX,    Br_XXX,   WB_XXX,   RegWr_N,  Mask_XXX, Wen_XXX,  Kill_N),
    CSRRSI -> List(     PC_4,   A_XXX,  B_XXX, Ext_XXXX,    ALU_XXX,    Br_XXX,   WB_XXX,   RegWr_N,  Mask_XXX, Wen_XXX,  Kill_N),
    CSRRCI -> List(     PC_4,   A_XXX,  B_XXX, Ext_XXXX,    ALU_XXX,    Br_XXX,   WB_XXX,   RegWr_N,  Mask_XXX, Wen_XXX,  Kill_N)
  )
}

class ControlSignals extends Bundle {
  val inst    = Input(UInt(32.W))
  val pc_sel  = Output(UInt(2.W))
  val A_sel   = Output(UInt(1.W))
  val B_sel   = Output(UInt(1.W))
  val imm_sel = Output(UInt(3.W))
  val alu_op  = Output(UInt(4.W))
  val br_type = Output(UInt(3.W))
  val wb_sel  = Output(UInt(2.W))
  val reg_wr  = Output(Bool())
  val mask    = Output(UInt(4.W))
  val wen     = Output(UInt(1.W))
  val kill    = Output(Bool())
}

class Control extends Module {
  val io          = IO(new ControlSignals)
  val ctrlSignals = ListLookup(io.inst, Control.default, Control.map)

  io.pc_sel := ctrlSignals(0)

  io.A_sel   := ctrlSignals(1)
  io.B_sel   := ctrlSignals(2)
  io.imm_sel := ctrlSignals(3)
  io.alu_op  := ctrlSignals(4)
  io.br_type := ctrlSignals(5)

  io.wb_sel := ctrlSignals(6)
  io.reg_wr := ctrlSignals(7).asBool
  io.mask   := ctrlSignals(8)
  io.wen    := ctrlSignals(9)
  io.kill   := ctrlSignals(10).asBool
}
