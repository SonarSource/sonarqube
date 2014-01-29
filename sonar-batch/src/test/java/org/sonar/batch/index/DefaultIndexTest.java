/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.index;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.ResourceFilter;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilters;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Library;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.Violation;
import org.sonar.api.violations.ViolationQuery;
import org.sonar.batch.ProjectTree;
import org.sonar.batch.ResourceFilters;
import org.sonar.batch.issue.DeprecatedViolations;
import org.sonar.batch.issue.ModuleIssues;
import org.sonar.core.component.ScanGraph;

import java.io.IOException;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultIndexTest {

  @org.junit.Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private DefaultIndex index = null;
  private DeprecatedViolations deprecatedViolations;
  private Rule rule;
  private RuleFinder ruleFinder;

  private Project project;

  private Project moduleA;

  private Project moduleB;

  private Project moduleB1;

  @Before
  public void createIndex() throws IOException {
    deprecatedViolations = mock(DeprecatedViolations.class);
    MetricFinder metricFinder = mock(MetricFinder.class);
    when(metricFinder.findByKey("ncloc")).thenReturn(CoreMetrics.NCLOC);
    ruleFinder = mock(RuleFinder.class);

    ProjectTree projectTree = mock(ProjectTree.class);
    index = new DefaultIndex(mock(PersistenceManager.class), projectTree, metricFinder, mock(ScanGraph.class), deprecatedViolations, mock(ResourceKeyMigration.class));

    java.io.File baseDir = temp.newFolder();
    project = new Project("project");
    when(projectTree.getProjectDefinition(project)).thenReturn(ProjectDefinition.create().setBaseDir(baseDir));
    moduleA = new Project("moduleA").setParent(project);
    when(projectTree.getProjectDefinition(moduleA)).thenReturn(ProjectDefinition.create().setBaseDir(new java.io.File(baseDir, "moduleA")));
    moduleB = new Project("moduleB").setParent(project);
    when(projectTree.getProjectDefinition(moduleB)).thenReturn(ProjectDefinition.create().setBaseDir(new java.io.File(baseDir, "moduleB")));
    moduleB1 = new Project("moduleB1").setParent(moduleB);
    when(projectTree.getProjectDefinition(moduleB1)).thenReturn(ProjectDefinition.create().setBaseDir(new java.io.File(baseDir, "moduleB/moduleB1")));

    ResourceFilter filter = new ResourceFilter() {

      public boolean isIgnored(Resource resource) {
        return StringUtils.containsIgnoreCase(resource.getKey(), "excluded");
      }
    };
    RulesProfile rulesProfile = RulesProfile.create();
    rule = Rule.create("repoKey", "ruleKey", "Rule");
    rule.setId(1);
    rulesProfile.activateRule(rule, null);
    index.setCurrentProject(project, new ResourceFilters(new ResourceFilter[] {filter}), mock(ModuleIssues.class));
    index.doStart(project);
  }

  @Test
  public void shouldIndexParentOfDeprecatedFiles() {
    File file = File.create("src/org/foo/Bar.java", "org/foo/Bar.java", null, false);
    assertThat(index.index(file)).isTrue();

    Directory reference = Directory.create("src/org/foo", "org/foo");
    assertThat(index.getResource(reference).getName()).isEqualTo("src/org/foo");
    assertThat(index.isIndexed(reference, true)).isTrue();
    assertThat(index.isExcluded(reference)).isFalse();
    assertThat(index.getChildren(reference)).hasSize(1);
    assertThat(index.getParent(reference)).isInstanceOf(Project.class);
  }

  @Test
  public void shouldIndexTreeOfResources() {
    Directory directory = Directory.create("src/org/foo", "org/foo");
    File file = File.create("src/org/foo/Bar.java", "org/foo/Bar.java", Java.INSTANCE, false);

    assertThat(index.index(directory)).isTrue();
    assertThat(index.index(file, directory)).isTrue();

    File fileRef = File.create("src/org/foo/Bar.java", "org/foo/Bar.java", null, false);
    assertThat(index.getResource(fileRef).getKey()).isEqualTo("src/org/foo/Bar.java");
    assertThat(index.getResource(fileRef).getDeprecatedKey()).isEqualTo("org/foo/Bar.java");
    assertThat(index.getResource(fileRef).getLanguage().getKey()).isEqualTo("java");
    assertThat(index.isIndexed(fileRef, true)).isTrue();
    assertThat(index.isExcluded(fileRef)).isFalse();
    assertThat(index.getChildren(fileRef)).isEmpty();
    assertThat(index.getParent(fileRef)).isInstanceOf(Directory.class);
  }

  @Test
  public void shouldIndexLibraryOutsideProjectTree() {
    Library lib = new Library("junit", "4.8");
    assertThat(index.index(lib)).isTrue();

    Library reference = new Library("junit", "4.8");
    assertThat(index.getResource(reference).getQualifier()).isEqualTo(Qualifiers.LIBRARY);
    assertThat(index.isIndexed(reference, true)).isTrue();
    assertThat(index.isExcluded(reference)).isFalse();
  }

  @Test
  public void shouldNotIndexResourceIfParentNotIndexed() {
    Directory directory = Directory.create("src/org/other", "org/other");
    File file = File.create("src/org/foo/Bar.java", "org/foo/Bar.java", null, false);

    assertThat(index.index(file, directory)).isFalse();

    File fileRef = File.create("src/org/foo/Bar.java", "org/foo/Bar.java", null, false);
    assertThat(index.isIndexed(directory, true)).isFalse();
    assertThat(index.isIndexed(fileRef, true)).isFalse();
    assertThat(index.isExcluded(fileRef)).isFalse();
    assertThat(index.getChildren(fileRef)).isEmpty();
    assertThat(index.getParent(fileRef)).isNull();
  }

  @Test
  public void shouldBeExcluded() {
    File file = File.create("src/org/foo/ExcludedBar.java", "org/foo/ExcludedBar.java", null, false);
    assertThat(index.index(file)).isFalse();
    assertThat(index.isIndexed(file, true)).isTrue();
    assertThat(index.isIndexed(file, false)).isFalse();
    assertThat(index.isExcluded(file)).isTrue();
  }

  @Test
  public void shouldIndexResourceWhenAddingMeasure() {
    Resource dir = Directory.create("src/org/foo", "org/foo");
    index.addMeasure(dir, new Measure("ncloc").setValue(50.0));

    assertThat(index.isIndexed(dir, true)).isTrue();
    assertThat(index.getMeasures(dir, MeasuresFilters.metric("ncloc")).getIntValue()).isEqualTo(50);
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2107
   */
  @Test
  public void shouldNotFailWhenSavingViolationOnNullRule() {
    File file = File.create("src/org/foo/Bar.java", "org/foo/Bar.java", null, false);
    Violation violation = Violation.create((Rule) null, file);
    index.addViolation(violation);

    assertThat(index.getViolations(file)).isEmpty();
  }

  /**
   * See https://jira.codehaus.org/browse/SONAR-3583
   */
  @Test
  public void should_ignore_violation_on_unknown_rules() {
    Rule ruleWithoutID = Rule.create("repoKey", "ruleKey", "Rule");

    File file = File.create("src/org/foo/Bar.java", "org/foo/Bar.java", null, false);
    Violation violation = Violation.create(ruleWithoutID, file);
    index.addViolation(violation);

    assertThat(index.getViolations(file)).isEmpty();
  }

  @Test
  public void should_get_violation() {
    Rule rule = Rule.create("repoKey", "ruleKey", "Rule");
    File file = File.create("src/org/foo/Bar.java", "org/foo/Bar.java", null, false);
    Violation violation = Violation.create(rule, file);
    when(deprecatedViolations.get(anyString())).thenReturn(newArrayList(violation));

    index.index(file);
    index.addViolation(violation);

    assertThat(index.getViolations(file)).hasSize(1);
  }

  @Test
  public void should_not_save_violation_if_resource_not_indexed() {
    Rule rule = Rule.create("repoKey", "ruleKey", "Rule");
    File file = File.create("src/org/foo/Bar.java", "org/foo/Bar.java", null, false);
    Violation violation = Violation.create(rule, file);
    when(deprecatedViolations.get(anyString())).thenReturn(newArrayList(violation));

    index.addViolation(violation);

    assertThat(index.getViolations(file)).hasSize(0);
  }

  @Test
  public void should_get_filtered_violation_with_off_switch_mode() {
    Rule rule = Rule.create("repoKey", "ruleKey", "Rule");
    File file = File.create("src/org/foo/Bar.java", "org/foo/Bar.java", null, false);
    Violation violation = Violation.create(rule, file).setSwitchedOff(true);

    when(deprecatedViolations.get(anyString())).thenReturn(newArrayList(violation));

    index.index(file);
    index.addViolation(violation);

    assertThat(index.getViolations(ViolationQuery.create().forResource(file).setSwitchMode(ViolationQuery.SwitchMode.OFF))).hasSize(1);
  }

  @Test
  public void should_get_filtered_violation_with_on_switch_mode() {
    Rule rule = Rule.create("repoKey", "ruleKey", "Rule");
    File file = File.create("src/org/foo/Bar.java", "org/foo/Bar.java", null, false);
    Violation violation = Violation.create(rule, file).setSwitchedOff(false);

    when(deprecatedViolations.get(anyString())).thenReturn(newArrayList(violation));

    index.index(file);
    index.addViolation(violation);

    assertThat(index.getViolations(ViolationQuery.create().forResource(file).setSwitchMode(ViolationQuery.SwitchMode.ON))).hasSize(1);
  }

  @Test
  public void shouldComputePathOfIndexedModules() {
    assertThat(index.getResource(project).getPath()).isNull();
    assertThat(index.getResource(moduleA).getPath()).isEqualTo("moduleA");
    assertThat(index.getResource(moduleB).getPath()).isEqualTo("moduleB");
    assertThat(index.getResource(moduleB1).getPath()).isEqualTo("moduleB1");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetViolationsWithQueryWithNoResource() {
    index.getViolations(ViolationQuery.create());
  }

}
