package org.sonar.tests;

public class SecondClass {
	
	public SecondClass(int i) {
		int j = i++;
	}
	
	public String foo() {
		return "hello";
	}
}