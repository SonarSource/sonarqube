package org.sonar.tests;

public class SONAR684EncodeViolationMessages {
	
	private void bar() {
      buz("<select>");
      buz("<select>");
      buz("<select>");
      buz("<select>");
      buz("<select>");
      buz("<select>");
      buz("<select>");
      buz("<select>");
      buz("<select>");
    }
    
    private void buz(String x) {}

}