---
title: Executable Lines
url: /extend/executable-lines/
---
 
These are the guidelines that SonarSource uses internally when defining executable lines for a language. Community plugins are not required to adhere to these guidelines. They are provided here only in case they are useful.

## Things that are executable
Executable lines data is used to calculate missing test coverage for files that are not included in coverage reports. Ideally, executable line counts will be at or just under what coverage engines would calculate.

Generally, each line containing a statement should count as an executable line, with the exception that compound statements ({}) are ignored, although their contents are not

So:
```
void doTheThing ()        // +0
{                         // +0
  String fname="Finn";    // +1
  etc();                  // +1
}                         // +0
```

## Things that are ignored
### !Statement: +0 
Since some coverage engines mark these things as executable, it's worth stating explicitly that we will ignore them:

* lines containing only punctuation: }, });, ;
* the method signature of a method definition

### Imports, Declarations: +0
Imports, package and namespace statements, declarations, and a few other things demonstrated below are ignored, 
```
package foo;     // +0
namespace bar {  // +0
  ...
}
  
import java.util.ArrayList;  // +0
#include <stdio>             // +0
  
public interface FooFace {  // +0
  void doFoo();             // +0
}
public class Foo1 implements FooFace {  // +0
  private String name;                  // +0
}
struct PairWithOperator { // +0
  int x;                  // +0
  int y;                  // +0
  
  bool operator==(PairWithOperator rhs) const {  // +0
    return x == rhs.x && y == rhs.y;             // +1
  }
}
  
class C {
  C(const C&) =default;  // +0 (explicit inheritance of parent method)
}
 
using Vec = std::vector<T,MyAllocator<T>>;       // +0
  
static {                 // +0
  ...
}
 
01  ERROR-MESSAGE.                                      *> +0
        02  ERROR-TEXT  PIC X(132) OCCURS 10 TIMES      *> +0
                                   INDEXED BY ERROR-INDEX.
77  ERROR-TEXT-LEN      PIC S9(9)  COMP VALUE +132.     *> +0
```

### Location
The presence of executable code on a line makes the entire line executable.

If a statement is split over multiple lines, the line to be marked executable is the first one with executable code. 
Given that a for loop is considered executable:
```
for         // +1
  (         // +0
   int i=0; // +0
   i < 10;  // +0
   i++      // +0
  )         // +0
{           // +0
}
```
Regardless of the number of lines across which nested statements are spread, the executable line count should only be incremented by one, since typically the execution of one naturally follows from the other. 

```
foo(1, bar());  // +1
  
foo(1,          // +1
    bar());     // +0
```
We ignore here the possibility that `bar()` could throw an exception, preventing `foo` from being executed.

## Exceptions
### Python
Based on observations from code on SonarCloud, `# pragma: no cover` exempts a block from coverage

![# pragma: no cover example](/images/executable-lines-python-exception.png)

### JavaScript
It seems to be accepted practice in JavaScript to mark variable declarations executable, so we will too. E.G.
```
var a;  // +1
```
