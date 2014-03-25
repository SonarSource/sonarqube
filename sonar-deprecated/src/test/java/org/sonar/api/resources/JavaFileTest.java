/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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

import java.util.List;

public class JavaFileTest {

  JavaFile javaFile = new JavaFile();

  @Test(expected = UnsupportedOperationException.class)
  public void testConstructor() {
    JavaFile javaClass = new JavaFile("", "");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testConstructor2() {
    JavaFile javaClass = new JavaFile("", "", true);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testConstructor3() {
    JavaFile javaClass = new JavaFile("");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testConstructor4() {
    JavaFile javaClass = new JavaFile("", true);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetParent() {
    javaFile.getParent();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetDescription() {
    javaFile.getDescription();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetLanguage() {
    javaFile.getLanguage();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetName() {
    javaFile.getName();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetLongName() {
    javaFile.getLongName();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetScope() {
    javaFile.getScope();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetQualifier() {
    javaFile.getQualifier();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testIsUnitTest() {
    javaFile.isUnitTest();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testMathFilePattern() {
    javaFile.matchFilePattern("");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void fromIoFile1() {
    JavaFile.fromIOFile(null, (Project) null, true);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void fromIoFile2() {
    JavaFile.fromIOFile(null, (List<java.io.File>) null, true);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void fromRelativePath() {
    JavaFile.fromRelativePath("", false);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void fromAbsolutePath() {
    JavaFile.fromAbsolutePath("", (List<java.io.File>) null, false);
  }
}
