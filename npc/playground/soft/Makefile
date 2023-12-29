.PHONY: all clean $(ALL)

ARCH ?= riscv32-npc
ALL ?= program/sort

all: $(addprefix Makefile-, $(ALL))
  @echo "" $(ALL)

$(ALL): %: Makefile-%

Makefile-%: %.c
  @/bin/echo -e "NAME = $*\nSRCS = $<\nLIBS += klib\nINC_PATH += $(shell pwd)/env/p $(shell pwd)/isa/macros/scalar\ninclude $${AM_HOME}/Makefile" > $@
  -@make -s -f $@ ARCH=$(ARCH) $(MAKECMDGOALS)
  -@rm -f Makefile-$*

clean:
  rm -rf Makefile-* build/