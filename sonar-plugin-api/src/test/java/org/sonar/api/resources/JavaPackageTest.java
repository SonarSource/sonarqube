/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.resources;

import org.junit.Test;

public class JavaPackageTest {

  JavaPackage javaPackage = new JavaPackage();

  @Test(expected = UnsupportedOperationException.class)
  public void testConstructor() {
    new JavaPackage("");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetParent() {
    javaPackage.getParent();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetDescription() {
    javaPackage.getDescription();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetLanguage() {
    javaPackage.getLanguage();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetName() {
    javaPackage.getName();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetLongName() {
    javaPackage.getLongName();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetScope() {
    javaPackage.getScope();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetQualifier() {
    javaPackage.getQualifier();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testIsUnitTest() {
    javaPackage.isDefault();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testMathFilePattern() {
    javaPackage.matchFilePattern("");
  }

}
