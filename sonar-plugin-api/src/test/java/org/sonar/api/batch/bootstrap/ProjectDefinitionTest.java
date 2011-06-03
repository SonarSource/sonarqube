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
package org.sonar.api.batch.bootstrap;

import org.junit.Test;
import org.sonar.api.CoreProperties;

import java.io.File;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ProjectDefinitionTest {

  @Test
  public void shouldSetKey() {
    ProjectDefinition def = new ProjectDefinition(new File("."), new File("."), new Properties());
    def.setKey("mykey");
    assertThat(def.getKey(), is("mykey"));
  }

  @Test
  public void shouldSetOptionalFields() {
    ProjectDefinition def = new ProjectDefinition(new File("."), new File("."), new Properties());
    def.setName("myname");
    def.setDescription("desc");
    assertThat(def.getName(), is("myname"));
    assertThat(def.getDescription(), is("desc"));
  }

  @Test
  public void shouldSupportDefaultName() {
    ProjectDefinition def = new ProjectDefinition(new File("."), new File("."), new Properties());
    def.setKey("myKey");
    assertThat(def.getName(), is("Unnamed - myKey"));
  }
  @Test
  public void shouldGetKeyFromProperties() {
    Properties props = new Properties();
    props.setProperty(CoreProperties.PROJECT_KEY_PROPERTY, "foo");
    ProjectDefinition def = new ProjectDefinition(new File("."), new File("."), props);
    assertThat(def.getKey(), is("foo"));
  }

  @Test
  public void testDefaultValues() {
    ProjectDefinition def = new ProjectDefinition(new File("."), new File("."), new Properties());
    assertThat(def.getSourceDirs().size(), is(0));
    assertThat(def.getTestDirs().size(), is(0));
    assertThat(def.getBinaries().size(), is(0));
    assertThat(def.getLibraries().size(), is(0));
  }

  @Test
  public void shouldAddDirectories() {
    ProjectDefinition def = new ProjectDefinition(new File("."), new File("."), new Properties());
    def.addSourceDirs("src/main/java", "src/main/java2");
    def.addTestDirs("src/test/java");
    def.addTestDirs("src/test/java2");
    def.addBinaryDir("target/classes");
    def.addBinaryDir("target/classes2");
    def.addLibrary("junit.jar");
    def.addLibrary("mockito.jar");

    assertFiles(def.getSourceDirs(), "src/main/java", "src/main/java2");
    assertFiles(def.getTestDirs(), "src/test/java", "src/test/java2");
    assertFiles(def.getBinaries(), "target/classes", "target/classes2");
    assertFiles(def.getLibraries(), "junit.jar", "mockito.jar");
  }

  @Test
  public void shouldAddFiles() {
    ProjectDefinition def = new ProjectDefinition(new File("."), new File("."), new Properties());
    def.addSourceFiles("src/main/java/foo/Bar.java", "src/main/java/hello/World.java");
    def.addTestFiles("src/test/java/foo/BarTest.java", "src/test/java/hello/WorldTest.java");

    assertFiles(def.getSourceFiles(), "src/main/java/foo/Bar.java", "src/main/java/hello/World.java");
    assertFiles(def.getTestFiles(), "src/test/java/foo/BarTest.java", "src/test/java/hello/WorldTest.java");

  }

  @Test
  public void shouldManageRelationships() {
    ProjectDefinition root = new ProjectDefinition(new File("."), new File("."), new Properties());
    ProjectDefinition child = new ProjectDefinition(new File("."), new File("."), new Properties());
    root.addSubProject(child);

    assertThat(root.getSubProjects().size(), is(1));
    assertThat(child.getSubProjects().size(), is(0));

    assertThat(root.getParent(), nullValue());
    assertThat(child.getParent(), is(root));
  }

  @Test
  public void shouldResetSourceDirs() {
    ProjectDefinition root = new ProjectDefinition(new File("."), new File("."), new Properties());
    root.addSourceDirs("src", "src2/main");
    assertThat(root.getSourceDirs().size(), is(2));

    root.resetSourceDirs();
    assertThat(root.getSourceDirs().size(), is(0));
  }

  @Test
  public void shouldResetTestDirs() {
    ProjectDefinition root = new ProjectDefinition(new File("."), new File("."), new Properties());
    root.addTestDirs("src", "src2/test");
    assertThat(root.getTestDirs().size(), is(2));

    root.resetTestDirs();
    assertThat(root.getTestDirs().size(), is(0));
  }

  private static void assertFiles(List<String> paths, String... values) {
    assertThat(paths.size(), is(values.length));
    for (int i = 0; i < values.length; i++) {
      assertThat(paths.get(i), is(values[i]));
    }
  }
}
