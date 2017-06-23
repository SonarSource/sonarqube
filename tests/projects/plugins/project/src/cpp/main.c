#include <stdio.h> /* NOK, according to the MISRA C 2004 20.9 rule, stdio.h must not be used in embedded system's production code */

#include <mylib.h>

/*
 * Compile & run from current folder:
 *   gcc -Wall -pedantic -std=c99 -I../lib -o main main.c && ./main
 */
 
int main(void) {
  int x = ADD(40, 2);
  
  if (x != 42)
  { /* NOK, empty code blocks generate violations */
  }

  printf("40 + 2 = %d\n", x);
}
