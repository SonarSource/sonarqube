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
package org.sonar.samples.duplicated_lines_with_other_package1;

public class DuplicatedLinesWithOtherPackage {

  public DuplicatedLinesWithOtherPackage() {
  }

  public void duplicatedMethodWithOtherPackage() {
    char[] charList = new char[30];
    charList[0] = 'a';
    charList[1] = 'b';
    charList[2] = 'c';
    charList[3] = 'd';
    charList[4] = 'e';
    charList[5] = 'f';
    charList[6] = 'g';
    charList[7] = 'g';
    charList[8] = 'g';
    charList[9] = 'g';
    charList[10] = 'g';
    charList[11] = 'g';
    charList[12] = 'g';
    charList[13] = 'g';
    charList[14] = 'g';
    charList[15] = 'g';
    charList[16] = 'g';
    charList[17] = 'g';
    charList[18] = 'g';
    charList[19] = 'g';
    charList[20] = 'g';
    charList[21] = 'g';
    charList[22] = 'g';
    charList[23] = 'g';
    charList[24] = 'g';
    charList[27] = 'g';
    charList[28] = 'g';
    charList[29] = 'g';
    charList[30] = 'g';
  }

}