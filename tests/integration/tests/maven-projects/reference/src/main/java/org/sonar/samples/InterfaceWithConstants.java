package org.sonar.samples;

public interface InterfaceWithConstants {
  int INT_CONSTANT = 1;

  EmptyClass OBJECT_CONSTANT = new EmptyClass();

  void doSomething();

  void doSomethingElse();
}
