BUILD_DIR = ./build
base_dir   = $(abspath .)
src_dir    = $(base_dir)/playground
gen_dir    = $(base_dir)/build
nproc      = $(shell nproc --ignore 1)
ysyxSoc_dir= $(base_dir)/../ysyxSoC

config_file = template.gtkw
waveform_file = dump.vcd

export PATH := $(PATH):$(abspath ./utils)

test:
	mill -i __.test

verilog:
	$(call git_commit, "generate verilog")
	mkdir -p $(BUILD_DIR)
	mill -i __.test.runMain Elaborate -td $(BUILD_DIR) 
	sed -i -e 's/_\(aw\|ar\|w\|r\|b\)_\(\|bits_\)/_\1/g' $(BUILD_DIR)/Tile.v

CXXFLAGS += -std=c++11 -Wall -Wno-unused-variable
LDFLAGS += -lreadline -ldl -pie

VERILATOR = verilator --cc --exe --timescale "1ns/1ns" --no-timing
VERILATOR_FLAGS = --assert -Wno-STMTDLY -O3 --trace --threads $(nproc)\
	--top-module ysyxSoCFull -Mdir $(gen_dir)/VysyxSoCFull.csrc \
	-CFLAGS "$(CXXFLAGS) -include $(gen_dir)/VysyxSoCFull.csrc/VysyxSoCFull.h" \
	-LDFLAGS "$(LDFLAGS)" \
	-I$(gen_dir) ./Tile.v \
	-I$(ysyxSoc_dir)/perip/uart16550/rtl \
	-I$(ysyxSoc_dir)/perip/spi/rtl ./spi_top_apb.v \
	-I$(ysyxSoc_dir)/perip/sdram \
	-I$(ysyxSoc_dir)/perip/vga \
	-I$(ysyxSoc_dir)/perip/ps2 \
	-I$(ysyxSoc_dir)/perip/gpio \
	-I$(ysyxSoc_dir)/perip/bitrev \
	-I$(ysyxSoc_dir)/perip/flash \
	-I$(ysyxSoc_dir)/perip/sdram/core_sdram_axi4 \
	-I$(ysyxSoc_dir)/perip/psram \
	-I$(ysyxSoc_dir)/perip/psram/efabless ./EF_PSRAM_CTRL_wb.v \
	-I$(ysyxSoc_dir)/perip/psram/efabless ./EF_PSRAM_CTRL.v \
	-I$(ysyxSoc_dir)/perip/amba 

$(base_dir)/VysyxSoCFull: $(ysyxSoc_dir)/build/ysyxSoCFull.v $(src_dir)/cc/top.cc $(src_dir)/cc/mm.cc $(src_dir)/cc/monitor/sdb.cc $(src_dir)/cc/monitor/watchpoint.cc $(src_dir)/cc/monitor/expr.cc
	$(VERILATOR) $(VERILATOR_FLAGS) -o $@ $< $(word 2, $^) $(word 3, $^) $(word 4, $^) $(word 6, $^)
	$(MAKE) -C $(gen_dir)/VysyxSoCFull.csrc -f VysyxSoCFull.mk

verilator: $(base_dir)/VysyxSoCFull

run:
	./VysyxSoCFull $(NEMU_HOME)/../am-kernels/tests/cpu-tests/build/add-longlong-riscv32e-npc.bin
	gtkwave $(waveform_file) $(config_file)


help:
	mill -i __.test.runMain Elaborate --help

compile:
	make clean
	make verilog
	make verilator

bsp:
	mill -i mill.bsp.BSP/install

reformat:
	mill -i __.reformat

checkformat:
	mill -i __.checkFormat

clean:
	-rm -rf $(BUILD_DIR)
	-rm -rf VysyxSoCFull
	-rm -rf dump.vcd

.PHONY: test verilog help compile bsp reformat checkformat clean

sim:
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	@echo "Write this Makefile by yourself."

include ../Makefile
