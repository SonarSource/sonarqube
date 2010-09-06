package org.sonar.tests.doNotImportSources;

public class Swiss {
  String test = "foo";

  public Swiss(){
    String t;
    if (true)
      t = "bar";
  }

  public boolean equals(Object o) {
	return false;
  }
}
