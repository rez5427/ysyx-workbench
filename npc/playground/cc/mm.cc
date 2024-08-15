#include <iostream>
#include <fstream>
#include <vector>

using namespace std;

void load_mem(uint8_t *mem, const char* fn) {
  int start = 0;

  //printf("fn: %s\n", fn);

  ifstream in(fn);

  if (!in)
  {
    std::cerr << "could not open " << fn << std::endl;
    exit(EXIT_FAILURE);
  }

  std::uint8_t value;
  while(in.read(reinterpret_cast<char*>(&value), sizeof(value))) {
    //printf("i: %d\n",start);
    //printf("value: %02x\n", value);
    mem[start++] = value;
    //printf("mem[start]: %02x\n", mem[start-1]);
  }
}