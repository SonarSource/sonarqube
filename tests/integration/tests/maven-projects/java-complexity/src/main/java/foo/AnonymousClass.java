package foo;

import java.io.Serializable;
import java.lang.Runnable;

public class AnonymousClass {

  // method complexity: 1 or 3 ?
  public void hasComplexAnonymousClass() {
    Runnable runnable = new Runnable() {
	
	  // method complexity: 2
      public void run() {
        if (true) {
          System.out.println("true");
        }
      }
    };
  }

  // method complexity: 1
  public void hasEmptyAnonymousClass() {
    Serializable serializable = new Serializable() {

    };
  }
}