package org.sonar.server.dummy;

public class DummyClass {
    
    public int add(int a, int b) {
        return a + b;
    }
    
    public boolean isPositive(int number) {
        if (number > 0) {
            return true;
        }
        return false;
    }
    
    public String getMessage() {
        return "Hello from DummyClass";
    }
}
