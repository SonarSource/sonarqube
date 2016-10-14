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
package org.sonar.scanner.index;

import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.measure.MetricFinder;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilters;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.scanner.DefaultProjectTree;
import org.sonar.scanner.FakeJava;
import org.sonar.scanner.scan.measure.MeasureCache;
import org.sonar.scanner.sensor.DefaultSensorStorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultIndexTest {

  @org.junit.Rule
  public TemporaryFolder temp = new TemporaryFolder();

  DefaultIndex index = null;
  Rule rule;
  RuleFinder ruleFinder;
  Project project;
  Project moduleA;
  Project moduleB;
  Project moduleB1;

  private java.io.File baseDir;

  @Before
  public void createIndex() throws IOException {
    ruleFinder = mock(RuleFinder.class);

    DefaultProjectTree projectTree = mock(DefaultProjectTree.class);
    BatchComponentCache resourceCache = new BatchComponentCache();
    index = new DefaultIndex(resourceCache, projectTree, mock(MeasureCache.class), mock(MetricFinder.class));

    baseDir = temp.newFolder();
    project = new Project("project");
    when(projectTree.getProjectDefinition(project)).thenReturn(ProjectDefinition.create().setBaseDir(baseDir));
    moduleA = new Project("moduleA").setParent(project);
    when(projectTree.getProjectDefinition(moduleA)).thenReturn(ProjectDefinition.create().setBaseDir(new java.io.File(baseDir, "moduleA")));
    moduleB = new Project("moduleB").setParent(project);
    when(projectTree.getProjectDefinition(moduleB)).thenReturn(ProjectDefinition.create().setBaseDir(new java.io.File(baseDir, "moduleB")));
    moduleB1 = new Project("moduleB1").setParent(moduleB);
    when(projectTree.getProjectDefinition(moduleB1)).thenReturn(ProjectDefinition.create().setBaseDir(new java.io.File(baseDir, "moduleB/moduleB1")));

    RulesProfile rulesProfile = RulesProfile.create();
    rule = Rule.create("repoKey", "ruleKey", "Rule");
    rule.setId(1);
    rulesProfile.activateRule(rule, null);
    index.setCurrentProject(project, mock(DefaultSensorStorage.class));
    index.doStart(project);
  }

  @Test
  public void shouldIndexParentOfDeprecatedFiles() {
    File file = File.create("src/org/foo/Bar.java", null, false);
    assertThat(index.index(file)).isTrue();

    Directory reference = Directory.create("src/org/foo");
    assertThat(index.getResource(reference).getName()).isEqualTo("src/org/foo");
    assertThat(index.getChildren(reference)).hasSize(1);
    assertThat(index.getParent(reference)).isInstanceOf(Project.class);
  }

  @Test
  public void shouldIndexTreeOfResources() {
    Directory directory = Directory.create("src/org/foo");
    File file = File.create("src/org/foo/Bar.java", FakeJava.INSTANCE, false);

    assertThat(index.index(directory)).isTrue();
    assertThat(index.index(file, directory)).isTrue();

    File fileRef = File.create("src/org/foo/Bar.java", null, false);
    assertThat(index.getResource(fileRef).getKey()).isEqualTo("src/org/foo/Bar.java");
    assertThat(index.getResource(fileRef).getLanguage().getKey()).isEqualTo("java");
    assertThat(index.getChildren(fileRef)).isEmpty();
    assertThat(index.getParent(fileRef)).isInstanceOf(Directory.class);
  }

  @Test
  public void shouldGetSource() throws Exception {
    Directory directory = Directory.create("src/org/foo");
    File file = File.create("src/org/foo/Bar.java", FakeJava.INSTANCE, false);
    FileUtils.write(new java.io.File(baseDir, "src/org/foo/Bar.java"), "Foo bar");

    assertThat(index.index(directory)).isTrue();
    assertThat(index.index(file, directory)).isTrue();

    File fileRef = File.create("src/org/foo/Bar.java", null, false);
    assertThat(index.getSource(fileRef)).isEqualTo("Foo bar");
  }

  @Test
  public void shouldNotIndexResourceIfParentNotIndexed() {
    Directory directory = Directory.create("src/org/other");
    File file = File.create("src/org/foo/Bar.java", null, false);

    assertThat(index.index(file, directory)).isFalse();

    File fileRef = File.create("src/org/foo/Bar.java", null, false);
    assertThat(index.getChildren(fileRef)).isEmpty();
    assertThat(index.getParent(fileRef)).isNull();
  }

  @Test
  public void shouldNotIndexResourceWhenAddingMeasure() {
    Resource dir = Directory.create("src/org/foo");
    index.addMeasure(dir, new Measure("ncloc").setValue(50.0));

    assertThat(index.getMeasures(dir, MeasuresFilters.metric("ncloc"))).isNull();
  }

  @Test
  public void shouldComputePathOfIndexedModules() {
    assertThat(index.getResource(project).getPath()).isNull();
    assertThat(index.getResource(moduleA).getPath()).isEqualTo("moduleA");
    assertThat(index.getResource(moduleB).getPath()).isEqualTo("moduleB");
    assertThat(index.getResource(moduleB1).getPath()).isEqualTo("moduleB1");
  }

}
