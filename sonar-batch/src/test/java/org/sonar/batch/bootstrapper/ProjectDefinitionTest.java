/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.bootstrapper;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Properties;

public class ProjectDefinitionTest {
  private ProjectDefinition def;

  @Before
  public void setUp() {
    def = new ProjectDefinition(new File("."), new File("."), new Properties());
  }

  @Test
  public void defaultValues() {
    assertThat(def.getSourceDirs().size(), is(0));
    assertThat(def.getTestDirs().size(), is(0));
    assertThat(def.getBinaries().size(), is(0));
    assertThat(def.getLibraries().size(), is(0));
  }

  @Test
  public void shouldAddDirectories() {
    def.addSourceDir("src/main/java");
    def.addSourceDir("src/main/java2");
    def.addTestDir("src/test/java");
    def.addTestDir("src/test/java2");
    def.addBinaryDir("target/classes");
    def.addBinaryDir("target/classes2");
    def.addLibrary("junit.jar");
    def.addLibrary("mockito.jar");

    assertDirs(def.getSourceDirs(), "src/main/java", "src/main/java2");
    assertDirs(def.getTestDirs(), "src/test/java", "src/test/java2");
    assertDirs(def.getBinaries(), "target/classes", "target/classes2");
    assertDirs(def.getLibraries(), "junit.jar", "mockito.jar");
  }

  private static void assertDirs(List<String> dirs, String... values) {
    assertThat(dirs.size(), is(values.length));
    for (int i = 0; i < values.length; i++) {
      assertThat(dirs.get(i), is(values[i]));
    }
  }
}
