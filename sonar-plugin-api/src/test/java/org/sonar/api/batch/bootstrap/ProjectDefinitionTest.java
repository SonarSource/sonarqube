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
package org.sonar.api.batch.bootstrap;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;

import java.io.File;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class ProjectDefinitionTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

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
    assertThat(def.sources().size(), is(0));
    assertThat(def.tests().size(), is(0));
  }

  /**
   * See SONAR-2879
   */
  @Test
  public void shouldTrimPaths() {
    ProjectDefinition def = ProjectDefinition.create();
    def.setProperty(ProjectDefinition.SOURCES_PROPERTY, "src1, src2 , with whitespace");
    def.setProperty(ProjectDefinition.TESTS_PROPERTY, "test1, test2 , with whitespace");


    assertFiles(def.sources(), "src1", "src2", "with whitespace");
    assertFiles(def.tests(), "test1", "test2", "with whitespace");
  }

  @Test
  public void shouldAddDirectoriesAsPath() {
    ProjectDefinition def = ProjectDefinition.create();
    def.addSources("src/main/java", "src/main/java2");
    def.addTests("src/test/java");
    def.addTests("src/test/java2");

    assertFiles(def.sources(), "src/main/java", "src/main/java2");
    assertFiles(def.tests(), "src/test/java", "src/test/java2");
  }

  @Test
  public void shouldAddDirectories() {
    ProjectDefinition def = ProjectDefinition.create();
    def.addSources(new File("src/main/java"), new File("src/main/java2"));
    def.addTests(new File("src/test/java"), new File("src/test/java2"));

    assertThat(def.sources().size(), is(2));
    assertThat(def.tests().size(), is(2));
  }

  @Test
  public void shouldAddFiles() {
    ProjectDefinition def = ProjectDefinition.create();
    def.addSources("src/main/java/foo/Bar.java", "src/main/java/hello/World.java");
    def.addTests("src/test/java/foo/BarTest.java", "src/test/java/hello/WorldTest.java");

    assertFiles(def.sources(), "src/main/java/foo/Bar.java", "src/main/java/hello/World.java");
    assertFiles(def.tests(), "src/test/java/foo/BarTest.java", "src/test/java/hello/WorldTest.java");
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
    root.addSources("src", "src2/main");
    assertThat(root.sources().size(), is(2));

    root.resetSources();
    assertThat(root.sources().size(), is(0));
  }

  @Test
  public void shouldResetTestDirs() {
    ProjectDefinition root = ProjectDefinition.create();
    root.addTests("src", "src2/test");
    assertThat(root.tests().size(), is(2));

    root.resetTests();
    assertThat(root.tests().size(), is(0));
  }

  private static void assertFiles(List<String> paths, String... values) {
    assertThat(paths.size(), is(values.length));
    for (int i = 0; i < values.length; i++) {
      assertThat(paths.get(i), is(values[i]));
    }
  }
}
