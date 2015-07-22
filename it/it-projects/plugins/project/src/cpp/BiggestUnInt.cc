// BiggestUnInt2.cc
//   usage:
//      BiggestUnInt2 <starting number> (optional argument, default is 1)
//   example:
//      BiggestUnInt2 4194305 
//   features:

#include <iostream>
#include <cstdlib>

using namespace std;

void bitsout( unsigned int n );

int main(int argc, char* argv[])
{
  int    N=0;
  unsigned int i=1 , oldi , j ;
  if(argc>1)   {
    sscanf( argv[1], "%u", &i ) ; // starting value
  }
  cout << "#\ti \ti+(i-1)\t2i\n" ; 
  cout << "#\t#### \t#######\t###\n" ; 
  do {
    oldi = i ;
    j = i-1 ;
    j += i ;   // this sets    j = 2i-1  (we hope)
    i *= 2 ;   // this doubles i         (we hope)
    cout << N << ":\t" << oldi << "\t" << j << "\t" << i << "\t";
    bitsout(i);
    cout << endl ;
    N++;
  } while ( j+1==i && i!=0 ) ; // keep going until something odd happens
  // (Under normal arithmetic,
  //  we always expect A: j+1 to equal i, and
  //  we always expect B: i not to be 0
  //  we keep going while _both_ A _and_ B are true.)
  //  (           '&&'   means    "_and_"           )
}

void bitsout( unsigned int m )
{
  int lastbit  ; 
  unsigned int two_to_power_i ;
  
  for ( int i = 31 ; i >= 0 ; i -- ) {
    two_to_power_i = (1<<i) ;
    lastbit = ( two_to_power_i & m ) == 0 ? 0 : 1 ;
    cout << lastbit ;
  }
}
