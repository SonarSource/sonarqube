package foo;

import java.io.Serializable;
import java.lang.Runnable;

// class complexity: 4
public class AnonymousClass {

  // method complexity: 3
  public void anonymousClassWithComplexity() {
    Runnable runnable = new Runnable() {
      public void run() {
        if (true) {
          System.out.println("true");
        }
      }
    };
  }

  // method complexity: 1
  public void anonymousClassWithZeroComplexity() {
    Serializable serializable = new Serializable() {

    };
  }
}