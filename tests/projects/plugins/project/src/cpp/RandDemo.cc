// RandDemo.cc
//   features:
//   * uses random() to get a random integer
//   * gets interactive user input
//   * uses ternary operator "q ? a : b"

#include <iostream>
#include <cstdlib>
#include <ctime>

using namespace std;

#define ranf() \
  ((double)random()/(1.0+(double)RAND_MAX)) // Uniform from interval [0,1) */

int main()
{
  int outcome, N=0, count_in=0 ;
  double fraction_in ;
  
  // Initialise random number generator with value of system time.
  srandom(time(NULL));
  
  // Get user input in correct range.
  while(N<1)
    {
      cout << "Input the number of experiments: ";
      cin >> N;
    }
  
  // Perform N experiments. 
  for(int n=1; n<=N; n++)
    {
      double x = ranf();
      double y = ranf();
      outcome = ( x*x + y*y > 1.0 ) ? 0 : 1 ; 
      if(outcome==1) count_in++;
      cout << outcome  << "\t" << x << "\t" << y << "\t"
	   << count_in << "\t" << n << endl;
    }
  
  // Sample goto to raise a violation
  goto L1;
  
  //Sample switch with default
  switch (bob)
    {
      case 1: {
          cout << "1";
          break;
      }
      case 2:
      {
          cout <<"2";
          break;
      }
      default:
      {
          cout << "3";
      }
  } 
  
  //Sample switch without default
  switch (bob)
    {
      case 1: {
          cout << "1";
          break;
      }
      case 2:
      {
          cout <<"2";
          break;
      }
  }
  
  //Integer variables must be converted (cast) for correct division
  fraction_in = static_cast<double>(count_in)/N;

  // Output results
  cout << "# Proportion of outcomes 'in' " 
       << fraction_in << endl;
  // Output results
  cout << "# pi-hat = " 
       << 4.0 * fraction_in << endl;
  return 0;
}
