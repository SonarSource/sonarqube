package org.sonar.samples;

public class Utf8Characters {
  String test = "voyelles accentuées";
  String test2 = "àéèûù";
  String test3 = "3 Ä";

  public Utf8Characters(){
    String t;
    if (true)
      t = "éöàä$£";
  }
}
