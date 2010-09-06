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
package org.sonar.samples.duplicated_lines_within_package;

public class DuplicatedLinesInSamePackage1 {

  public DuplicatedLinesInSamePackage1() {
  }

  /*
   * sddfgdfgfg
   */
  public void duplicatedMethodInSameClass1() {
    int intergerToBeIncremented = 0;
    while (intergerToBeIncremented < 100) {
      intergerToBeIncremented++;
    }
    int intergerToBeIncremented2 = 0;
    while (intergerToBeIncremented2 < 100) {
      intergerToBeIncremented2++;
    }
    // first
    int intergerToBeIncremented3 = 0;
    while (intergerToBeIncremented3 < 100) {
      intergerToBeIncremented3++;
    }
    int intergerToBeIncremented4 = 0;
    while (intergerToBeIncremented4 < 100) {
      intergerToBeIncremented4++;
    }
    int intergerToBeIncremented5 = 0;
    while (intergerToBeIncremented5 < 100) {
      intergerToBeIncremented5++;
    }
    int intergerToBeIncremented6 = 0;
    while (intergerToBeIncremented6 < 100) {
      intergerToBeIncremented6++;
    }
    int intergerToBeIncremented7 = 0;
    while (intergerToBeIncremented7 < 100) {
      intergerToBeIncremented7++;
    }
  }


}