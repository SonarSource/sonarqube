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
package duplicated_same_lines_within_3_classes;

public class Class2 {
  
  public void duplicatedMethod() {
    int intergerToBeIncremented = 0;
    while (intergerToBeIncremented < 100) {
      intergerToBeIncremented++;
    }
    System.out.println("test");
    int intergerToBeIncremented3 = 0;
    while (intergerToBeIncremented3 < 100) {
      intergerToBeIncremented3++;
    }
    System.out.println("test");
    int intergerToBeIncremented4 = 0;
    while (intergerToBeIncremented4 < 100) {
      intergerToBeIncremented4++;
    }
    System.out.println("test");
    int intergerToBeIncremented5 = 0;
    while (intergerToBeIncremented5 < 100) {
      intergerToBeIncremented5++;
    }
    System.out.println("test");
    int intergerToBeIncremented6 = 0;
    while (intergerToBeIncremented6 < 100) {
      intergerToBeIncremented6++;
    }
    System.out.println("test");
    int intergerToBeIncremented7 = 0;
    while (intergerToBeIncremented7 < 100) {
      intergerToBeIncremented7++;
    }
  }
  
  public void someOtherMethod() {
    System.out.println("Test2");
  }
}
