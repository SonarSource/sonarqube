/*
 * Sonar, entreprise quality control tool.
 * Copyright (C) 2007-2008 Hortis-GRC SA
 * mailto:be_agile HAT hortis DOT ch
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
package org.sonar.samples;

public class InnerClass {

  protected void methodOne() {
    int i = 0;
    i++;
  }

  protected void methodTwo() {
    int i = 0;
    i++;
  }

  protected int methodReturnThree() {
    return 3;
  }

  class InnerClassInside {

    InnerClassInside() {
    }

    protected void innerMethodOne() {
      System.out.println("in one");
    }

    protected void innerMethodTwo() {
      System.out.println("in two");
    }

    protected int methodReturnFour() {
      return 4;
    }

  }
}

class PrivateClass {
  PrivateClass() {

  }

  void innerMethodThree() {
    System.out.println("in three");
  }

  void innerMethodFour() {
    System.out.println("in four");
  }

  void innerMethodFive() {
    if (true) {
      System.out.println("in five");
    }
  }

  void innerMethodSix() {
    if (true) {
      System.out.println("in six");
    }
  }

  int methodReturnfive() {
    return 5;
  }

}