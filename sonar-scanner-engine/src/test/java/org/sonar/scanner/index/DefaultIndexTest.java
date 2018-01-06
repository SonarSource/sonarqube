/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.scanner.index;

import java.io.IOException;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputDir;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.InputComponentTree;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.scanner.scan.filesystem.InputComponentStore;
import org.sonar.scanner.scan.measure.MeasureCache;
import org.sonar.scanner.sensor.DefaultSensorStorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultIndexTest {

  @org.junit.Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private DefaultIndex index = null;
  private Rule rule;
  private RuleFinder ruleFinder;
  private Project project;
  private Project moduleA;
  private Project moduleB;
  private Project moduleB1;
  private InputComponentStore componentStore;
  private InputComponentTree componentTree;

  private java.io.File baseDir;

  @Before
  public void createIndex() throws IOException {
    ruleFinder = mock(RuleFinder.class);
    componentStore = mock(InputComponentStore.class);
    componentTree = mock(InputComponentTree.class);
    index = new DefaultIndex(componentStore, componentTree, mock(MeasureCache.class), mock(MetricFinder.class));

    baseDir = temp.newFolder();

    ProjectDefinition rootDef = ProjectDefinition.create().setKey("project").setBaseDir(baseDir).setWorkDir(temp.newFolder());
    java.io.File moduleABaseDir = new java.io.File(baseDir, "moduleA");
    moduleABaseDir.mkdir();
    ProjectDefinition moduleADef = ProjectDefinition.create().setKey("moduleA").setBaseDir(moduleABaseDir).setWorkDir(temp.newFolder());
    java.io.File moduleBBaseDir = new java.io.File(baseDir, "moduleB");
    moduleBBaseDir.mkdir();
    ProjectDefinition moduleBDef = ProjectDefinition.create().setKey("moduleB").setBaseDir(moduleBBaseDir).setWorkDir(temp.newFolder());
    java.io.File moduleB1BaseDir = new java.io.File(baseDir, "moduleB/moduleB1");
    moduleB1BaseDir.mkdir();
    ProjectDefinition moduleB1Def = ProjectDefinition.create().setKey("moduleB1").setBaseDir(moduleB1BaseDir).setWorkDir(temp.newFolder());

    rootDef.addSubProject(moduleADef);
    rootDef.addSubProject(moduleBDef);
    moduleBDef.addSubProject(moduleB1Def);

    project = new Project(new DefaultInputModule(rootDef));
    moduleA = new Project(new DefaultInputModule(moduleADef));
    moduleB = new Project(new DefaultInputModule(moduleBDef));
    moduleB1 = new Project(new DefaultInputModule(moduleB1Def));

    RulesProfile rulesProfile = RulesProfile.create();
    rule = Rule.create("repoKey", "ruleKey", "Rule");
    rule.setId(1);
    rulesProfile.activateRule(rule, null);
    index.setCurrentStorage(mock(DefaultSensorStorage.class));
  }

  @Test
  public void shouldGetHierarchy() throws IOException {
    InputComponent component = new DefaultInputModule(ProjectDefinition.create().setKey("module1").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder()));
    InputFile file1 = new TestInputFileBuilder("module1", "src/org/foo/Bar.java").build();

    when(componentStore.getByKey("module1")).thenReturn(component);
    when(componentStore.getByKey("module1:src/org/foo/Bar.java")).thenReturn(file1);
    when(componentTree.getParent(file1)).thenReturn(component);
    when(componentTree.getChildren(component)).thenReturn(Collections.singleton(file1));

    assertThat(index.getParent("module1:src/org/foo/Bar.java").getKey()).isEqualTo("module1");
    assertThat(index.getParent("module1")).isNull();

    assertThat(index.getChildren("module1")).containsOnly(File.create("src/org/foo/Bar.java"));
    assertThat(index.getChildren("module1:src/org/foo/Bar.java")).isEmpty();
  }

  @Test
  public void shouldTransformToResource() throws IOException {
    DefaultInputModule component = new DefaultInputModule(ProjectDefinition.create()
      .setKey("module1")
      .setProperty(CoreProperties.PROJECT_BRANCH_PROPERTY, "branch1")
      .setBaseDir(temp.newFolder())
      .setWorkDir(temp.newFolder()), 1);
    InputFile file1 = new TestInputFileBuilder("module1", "src/org/foo/Bar.java").build();
    InputDir dir = new DefaultInputDir("module1", "src");

    assertThat(index.toResource(component)).isInstanceOf(Project.class);
    assertThat(index.toResource(component).getKey()).isEqualTo("module1");
    assertThat(index.toResource(component).getEffectiveKey()).isEqualTo("module1:branch1");

    assertThat(index.toResource(file1)).isInstanceOf(File.class);
    assertThat(index.toResource(file1).getKey()).isEqualTo("src/org/foo/Bar.java");
    assertThat(index.toResource(file1).getPath()).isEqualTo("src/org/foo/Bar.java");

    assertThat(index.toResource(dir)).isInstanceOf(Directory.class);
    assertThat(index.toResource(dir).getKey()).isEqualTo("src");
    assertThat(index.toResource(dir).getPath()).isEqualTo("src");
  }
}
