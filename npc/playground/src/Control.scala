import chisel3._
import chisel3.util._

object Control {
    // pc_sel
    val PC_4 = 0.U(2.W)
    val PC_ALU = 1.U(2.W)

    // ExtOP
    val Ext_XXXX = 0.U(2.W)
    val Ext_immI = 1.U(3.W)
    val Ext_immU = 2.U(3.W)
    val Ext_immS = 3.U(3.W)
    val Ext_immB = 4.U(3.W)
    val Ext_immJ = 5.U(3.W)

    // RegWr
    val RegWr_N = 0.U(1.W)
    val RegWr_Y = 1.U(1.W)

    // ALUASrc
    val A_XXX = 0.U(1.W)
    val A_RS1 = 0.U(1.W)
    val A_PC = 1.U(1.W)

    // ALUBSrc
    val B_XXX = 0.U(1.W)
    val B_RS2 = 0.U(1.W)
    val B_Imm = 1.U(1.W)
    
    // Branch
    val Br_XXX = 0.U(3.W)
    val Br_Eq = 1.U(3.W)
    val Br_Neq = 2.U(3.W)
    val Br_Lt = 3.U(3.W)
    val Br_Ge = 4.U(3.W)
    val Br_Ltu = 5.U(3.W)
    val Br_Geu = 6.U(3.W)

    // wb_sel
    val WB_XXX = 0.U(2.W)
    val WB_ALU = 0.U(2.W)
    val WB_MEM = 1.U(2.W)
    val WB_PC4 = 2.U(2.W)

    // mask
    val Mask_XXX = 0.U(4.W)
    val Mask_B = 1.U(4.W)
    val Mask_H = 3.U(4.W)
    val Mask_W = 15.U(4.W)

    import Alu._
    import Instructions._

    val default =
  //                                                                                      
  //                  pc_sel  A_sel   B_sel   imm_sel     alu_op      br_type   wb_sel     wb_en     mask
  //                     |      |       |        |           |          |         |          |        |
                  List(PC_4,  A_XXX,  B_XXX,  Ext_XXXX,   ALU_XXX,    Br_XXX,   WB_XXX,   RegWr_N,  Mask_XXX)
    val map = Array(
        LUI ->    List(PC_4,  A_PC,   B_Imm,  Ext_immU,   ALU_COPY_B, Br_XXX,   WB_ALU,   RegWr_N,  Mask_XXX),
        AUIPC ->  List(PC_4,  A_PC,   B_Imm,  Ext_immU,   ALU_ADD,    Br_XXX,   WB_ALU,   RegWr_N,  Mask_XXX),
        JAL ->    List(PC_ALU,A_PC,   B_Imm,  Ext_immJ,   ALU_ADD,    Br_XXX,   WB_PC4,   RegWr_Y,  Mask_XXX),
        JALR ->   List(PC_ALU,A_RS1,  B_Imm,  Ext_immI,   ALU_ADD,    Br_XXX,   WB_PC4,   RegWr_Y,  Mask_XXX),
        BEQ ->    List(PC_ALU,A_RS1,  B_RS2,  Ext_immB,   ALU_ADD,    Br_Eq,    WB_ALU,   RegWr_N,  Mask_XXX),
        BNE ->    List(PC_ALU,A_RS1,  B_RS2,  Ext_immB,   ALU_ADD,    Br_Neq,   WB_ALU,   RegWr_N,  Mask_XXX),
        BLT ->    List(PC_ALU,A_RS1,  B_RS2,  Ext_immB,   ALU_ADD,    Br_Lt,    WB_ALU,   RegWr_N,  Mask_XXX),
        BGE ->    List(PC_ALU,A_RS1,  B_RS2,  Ext_immB,   ALU_ADD,    Br_Ge,    WB_ALU,   RegWr_N,  Mask_XXX),
        BLTU ->   List(PC_ALU,A_RS1,  B_RS2,  Ext_immB,   ALU_ADD,    Br_Ltu,   WB_ALU,   RegWr_N,  Mask_XXX),
        BGEU ->   List(PC_ALU,A_RS1,  B_RS2,  Ext_immB,   ALU_ADD,    Br_Geu,   WB_ALU,   RegWr_N,  Mask_XXX),
        LB ->     List(PC_ALU,A_RS1,  B_Imm,  Ext_immI,   ALU_ADD,    Br_XXX,   WB_MEM,   RegWr_Y,  Mask_B  ),
        LH ->     List(PC_ALU,A_RS1,  B_Imm,  Ext_immI,   ALU_ADD,    Br_XXX,   WB_MEM,   RegWr_Y,  Mask_H  ),
        LW ->     List(PC_ALU,A_RS1,  B_Imm,  Ext_immI,   ALU_ADD,    Br_XXX,   WB_MEM,   RegWr_Y,  Mask_W  ),
        LBU ->    List(PC_ALU,A_RS1,  B_Imm,  Ext_immI,   ALU_ADD,    Br_XXX,   WB_MEM,   RegWr_Y,  Mask_XXX),
        LHU ->    List(PC_ALU,A_RS1,  B_Imm,  Ext_immI,   ALU_ADD,    Br_XXX,   WB_MEM,   RegWr_Y,  Mask_XXX),
        SB ->     List(PC_ALU,A_RS1,  B_Imm,  Ext_immS,   ALU_ADD,    Br_XXX,   WB_XXX,   RegWr_N,  Mask_B  ),
        SH ->     List(PC_ALU,A_RS1,  B_Imm,  Ext_immS,   ALU_ADD,    Br_XXX,   WB_XXX,   RegWr_N,  Mask_H  ),
        SW ->     List(PC_ALU,A_RS1,  B_Imm,  Ext_immS,   ALU_ADD,    Br_XXX,   WB_XXX,   RegWr_N,  Mask_W  ),
        ADDI ->   List(PC_4,  A_RS1,  B_Imm,  Ext_immI,   ALU_ADD,    Br_XXX,   WB_ALU,   RegWr_N,  Mask_XXX),
        SLTI ->   List(PC_4,  A_RS1,  B_Imm,  Ext_immI,   ALU_SLT,    Br_XXX,   WB_ALU,   RegWr_N,  Mask_XXX),
        SLTIU ->  List(PC_4,  A_RS1,  B_Imm,  Ext_immI,   ALU_SLTU,   Br_XXX,   WB_ALU,   RegWr_N,  Mask_XXX),
        XORI ->   List(PC_4,  A_RS1,  B_Imm,  Ext_immI,   ALU_XOR,    Br_XXX,   WB_ALU,   RegWr_N,  Mask_XXX),
        ORI ->    List(PC_4,  A_RS1,  B_Imm,  Ext_immI,   ALU_OR,     Br_XXX,   WB_ALU,   RegWr_N,  Mask_XXX),
        ANDI ->   List(PC_4,  A_RS1,  B_Imm,  Ext_immI,   ALU_AND,    Br_XXX,   WB_ALU,   RegWr_N,  Mask_XXX),  
        SLLI ->   List(PC_4,  A_RS1,  B_Imm,  Ext_immI,   ALU_SLL,    Br_XXX,   WB_ALU,   RegWr_N,  Mask_XXX),
        SRLI ->   List(PC_4,  A_RS1,  B_Imm,  Ext_immI,   ALU_SRL,    Br_XXX,   WB_ALU,   RegWr_N,  Mask_XXX),
        SRAI ->   List(PC_4,  A_RS1,  B_Imm,  Ext_immI,   ALU_SRA,    Br_XXX,   WB_ALU,   RegWr_N,  Mask_XXX),
        ADD ->    List(PC_4,  A_RS1,  B_RS2,  Ext_XXXX,   ALU_ADD,    Br_XXX,   WB_ALU,   RegWr_N,  Mask_XXX),
        SUB ->    List(PC_4,  A_RS1,  B_RS2,  Ext_XXXX,   ALU_SUB,    Br_XXX,   WB_ALU,   RegWr_N,  Mask_XXX),
        SLL ->    List(PC_4,  A_RS1,  B_RS2,  Ext_XXXX,   ALU_SLL,    Br_XXX,   WB_ALU,   RegWr_N,  Mask_XXX),
        SLT ->    List(PC_4,  A_RS1,  B_RS2,  Ext_XXXX,   ALU_SLT,    Br_XXX,   WB_ALU,   RegWr_N,  Mask_XXX),
        SLTU ->   List(PC_4,  A_RS1,  B_RS2,  Ext_XXXX,   ALU_SLTU,   Br_XXX,   WB_ALU,   RegWr_N,  Mask_XXX),
        XOR ->    List(PC_4,  A_RS1,  B_RS2,  Ext_XXXX,   ALU_XOR,    Br_XXX,   WB_ALU,   RegWr_N,  Mask_XXX),
        SRL ->    List(PC_4,  A_RS1,  B_RS2,  Ext_XXXX,   ALU_SRL,    Br_XXX,   WB_ALU,   RegWr_N,  Mask_XXX),
        SRA ->    List(PC_4,  A_RS1,  B_RS2,  Ext_XXXX,   ALU_SRA,    Br_XXX,   WB_ALU,   RegWr_N,  Mask_XXX),
        OR ->     List(PC_4,  A_RS1,  B_RS2,  Ext_XXXX,   ALU_OR,     Br_XXX,   WB_ALU,   RegWr_N,  Mask_XXX),
        AND ->    List(PC_4,  A_RS1,  B_RS2,  Ext_XXXX,   ALU_AND,    Br_XXX,   WB_ALU,   RegWr_N,  Mask_XXX),
        FENCE ->  List(PC_4,  A_XXX,  B_XXX,  Ext_XXXX,   ALU_XXX,    Br_XXX,   WB_XXX,   RegWr_N,  Mask_XXX),
        FENCEI -> List(PC_4,  A_XXX,  B_XXX,  Ext_XXXX,   ALU_XXX,    Br_XXX,   WB_XXX,   RegWr_N,  Mask_XXX),
        ECALL ->  List(PC_4,  A_XXX,  B_XXX,  Ext_XXXX,   ALU_XXX,    Br_XXX,   WB_XXX,   RegWr_N,  Mask_XXX),
        CSRRW ->  List(PC_4,  A_XXX,  B_XXX,  Ext_XXXX,   ALU_XXX,    Br_XXX,   WB_XXX,   RegWr_N,  Mask_XXX),
        CSRRS ->  List(PC_4,  A_XXX,  B_XXX,  Ext_XXXX,   ALU_XXX,    Br_XXX,   WB_XXX,   RegWr_N,  Mask_XXX),
        CSRRC ->  List(PC_4,  A_XXX,  B_XXX,  Ext_XXXX,   ALU_XXX,    Br_XXX,   WB_XXX,   RegWr_N,  Mask_XXX),
        CSRRWI -> List(PC_4,  A_XXX,  B_XXX,  Ext_XXXX,   ALU_XXX,    Br_XXX,   WB_XXX,   RegWr_N,  Mask_XXX),
        CSRRSI -> List(PC_4,  A_XXX,  B_XXX,  Ext_XXXX,   ALU_XXX,    Br_XXX,   WB_XXX,   RegWr_N,  Mask_XXX),
        CSRRCI -> List(PC_4,  A_XXX,  B_XXX,  Ext_XXXX,   ALU_XXX,    Br_XXX,   WB_XXX,   RegWr_N,  Mask_XXX)
    )
}

class ControlSignals extends Bundle {
  val inst = Input(UInt(32.W))
  val pc_sel = UInt(2.W)
  val A_sel = UInt(1.W)
  val B_sel = UInt(1.W)
  val imm_sel = UInt(3.W)
  val alu_op = UInt(4.W)
  val br_type = UInt(3.W)
  val wb_sel = UInt(2.W)
  val wb_en = UInt(1.W)
  val mask = UInt(4.W)
}

class Control extends Module {
    val io = IO(new ControlSignals)
    val ctrlSignals = ListLookup(io.inst, Control.default, Control.map)

    io.pc_sel := ctrlSignals(0)

    io.A_sel := ctrlSignals(1)
    io.B_sel := ctrlSignals(2)
    io.imm_sel := ctrlSignals(3)
    io.alu_op := ctrlSignals(4)
    io.br_type := ctrlSignals(5)

    io.wb_sel := ctrlSignals(6)
    io.wb_en := ctrlSignals(7).asBool
    io.mask := ctrlSignals(8)
}