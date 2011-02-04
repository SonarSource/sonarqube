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

/**
 * See http://java.sun.com/docs/books/tutorial/java/javaOO/nested.html
 */
public class WithStaticInnerClass {
  public String mainMethod1() {
    String foo = new StaticNestedClass().staticNestedMethod1();
    foo += "xxxxxxxx";
    return foo;
  }

  public void mainMethod2() {
    int i = 9999;
    //do nothing
  }

  static class StaticNestedClass {
    public String staticNestedMethod1() {
      String s = "foo";
      return s;
    }
  }

  public static class PublicStaticNestedClass {
    public int publicStaticNestedMethod1() {
      String s = "bar";
      return 123456;
    }
  }
}
