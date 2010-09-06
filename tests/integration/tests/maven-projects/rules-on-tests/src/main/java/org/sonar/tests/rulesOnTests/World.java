package org.sonar.tests.rulesOnTests;

/**
 * 
 * THE JAVADOC HEADER
 *
 */
public class World {

    public World(int i) {
	    // single comment
		int j = i++;
	}
	
	private String myMethod() {
		/*
		comment
		on
		many
		lines		
		*/
		int toto=34; // comment at end of line
		return "hello";
	}
}
