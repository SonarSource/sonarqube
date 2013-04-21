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
package org.sonar.api.batch.bootstrap;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.sonar.api.CoreProperties;

public class ProjectDefinitionTest {

  @Test
  public void shouldSetKey() {
    ProjectDefinition def = ProjectDefinition.create();
    def.setKey("mykey");
    assertThat(def.getKey(), is("mykey"));
  }

  @Test
  public void shouldSetVersion() {
    ProjectDefinition def = ProjectDefinition.create();
    def.setVersion("2.0-SNAPSHOT");
    assertThat(def.getVersion(), is("2.0-SNAPSHOT"));
  }

  /**
   * Compatibility with Ant task.
   */
  @Test
  public void shouldNotCloneProperties() {
    Properties props = new Properties();

    ProjectDefinition def = ProjectDefinition.create(props);
    assertThat(def.getKey(), nullValue());

    props.setProperty(CoreProperties.PROJECT_KEY_PROPERTY, "mykey");
    assertThat(def.getKey(), is("mykey"));
  }

  @Test
  public void shouldSetOptionalFields() {
    ProjectDefinition def = ProjectDefinition.create();
    def.setName("myname");
    def.setDescription("desc");
    assertThat(def.getName(), is("myname"));
    assertThat(def.getDescription(), is("desc"));
  }

  @Test
  public void shouldSupportDefaultName() {
    ProjectDefinition def = ProjectDefinition.create();
    def.setKey("myKey");
    assertThat(def.getName(), is("Unnamed - myKey"));
  }

  @Test
  public void shouldGetKeyFromProperties() {
    Properties props = new Properties();
    props.setProperty(CoreProperties.PROJECT_KEY_PROPERTY, "foo");
    ProjectDefinition def = ProjectDefinition.create();
    def.setProperties(props);
    assertThat(def.getKey(), is("foo"));
  }

  @Test
  public void testDefaultValues() {
    ProjectDefinition def = ProjectDefinition.create();
    assertThat(def.getSourceDirs().size(), is(0));
    assertThat(def.getTestDirs().size(), is(0));
    assertThat(def.getBinaries().size(), is(0));
    assertThat(def.getLibraries().size(), is(0));
  }

  /**
   * See SONAR-2879
   */
  @Test
  public void shouldTrimPaths() {
    ProjectDefinition def = ProjectDefinition.create();
    def.setProperty(ProjectDefinition.SOURCE_DIRS_PROPERTY, "src1, src2 , with whitespace");
    def.setProperty(ProjectDefinition.TEST_DIRS_PROPERTY, "test1, test2 , with whitespace");
    def.setProperty(ProjectDefinition.BINARIES_PROPERTY, "bin1, bin2 , with whitespace");
    def.setProperty(ProjectDefinition.LIBRARIES_PROPERTY, "lib1, lib2 , with whitespace");

    assertFiles(def.getSourceDirs(), "src1", "src2", "with whitespace");
    assertFiles(def.getTestDirs(), "test1", "test2", "with whitespace");
    assertFiles(def.getBinaries(), "bin1", "bin2", "with whitespace");
    assertFiles(def.getLibraries(), "lib1", "lib2", "with whitespace");
  }

  @Test
  public void shouldAddDirectoriesAsPath() {
    ProjectDefinition def = ProjectDefinition.create();
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
  public void shouldAddDirectories() {
    ProjectDefinition def = ProjectDefinition.create();
    def.addSourceDirs(new File("src/main/java"), new File("src/main/java2"));
    def.addTestDirs(new File("src/test/java"), new File("src/test/java2"));
    def.addBinaryDir(new File("target/classes"));

    assertThat(def.getSourceDirs().size(), is(2));
    assertThat(def.getTestDirs().size(), CoreMatchers.is(2));
    assertThat(def.getBinaries().size(), CoreMatchers.is(1));
  }

  @Test
  public void shouldAddFiles() {
    ProjectDefinition def = ProjectDefinition.create();
    def.addSourceFiles("src/main/java/foo/Bar.java", "src/main/java/hello/World.java");
    def.addTestFiles("src/test/java/foo/BarTest.java", "src/test/java/hello/WorldTest.java");

    assertFiles(def.getSourceFiles(), "src/main/java/foo/Bar.java", "src/main/java/hello/World.java");
    assertFiles(def.getTestFiles(), "src/test/java/foo/BarTest.java", "src/test/java/hello/WorldTest.java");
  }

  @Test
  public void shouldManageRelationships() {
    ProjectDefinition root = ProjectDefinition.create();
    ProjectDefinition child = ProjectDefinition.create();
    root.addSubProject(child);

    assertThat(root.getSubProjects().size(), is(1));
    assertThat(child.getSubProjects().size(), is(0));

    assertThat(root.getParent(), nullValue());
    assertThat(child.getParent(), is(root));
  }

  @Test
  public void shouldResetSourceDirs() {
    ProjectDefinition root = ProjectDefinition.create();
    root.addSourceDirs("src", "src2/main");
    assertThat(root.getSourceDirs().size(), is(2));

    root.resetSourceDirs();
    assertThat(root.getSourceDirs().size(), is(0));
  }

  @Test
  public void shouldResetTestDirs() {
    ProjectDefinition root = ProjectDefinition.create();
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
