/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

public class WithSubClass {
  public void mainMethod1() {
    String foo = new SubClass1().subMethod();
    foo += "bar";
  }

  public void mainMethod2() {
    int i = 9999;
    //do nothing
  }
}

class SubClass1 {
  public String subMethod() {
    String foo = "foo";
    foo += "xxxxxxxx";
    return foo;
  }
}

class SubClass2 {
  public void subMethod() {
    String foo = "foo";
    foo += "xxxxxxxx";
  }
}
