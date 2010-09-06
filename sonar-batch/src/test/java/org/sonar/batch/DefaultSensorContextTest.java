/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.batch;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.design.Dependency;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.measures.RuleMeasure;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.JavaPackage;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.RulePriority;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import javax.persistence.Query;
import java.text.ParseException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

@Ignore
public class DefaultSensorContextTest extends AbstractDbUnitTestCase {
  private DefaultSensorContext context;
  private Project project;

  @Before
  public void before() {
    project = null;
    context = null;
  }

  @Test
  public void saveProjectMeasure() throws ParseException {
    setup("saveProjectMeasure");
    context.saveMeasure(CoreMetrics.NCLOC, 500.0);
    check("saveProjectMeasure", "projects", "snapshots", "project_measures");
  }

  @Test
  public void saveMeasureOnExistingResource() throws ParseException {
    setup("saveMeasureOnExistingResource");
    context.saveMeasure(new JavaPackage("org.sonar"), CoreMetrics.NCLOC, 200.0);
    check("saveMeasureOnExistingResource", "projects", "snapshots", "project_measures");
  }

  @Test
  public void avoidConflictWithResourceFromOtherProject() throws ParseException {
    setup("avoidConflictWithResourceFromOtherProject");
    context.saveMeasure(new JavaPackage("org.sonar"), CoreMetrics.NCLOC, 200.0);
    context.saveMeasure(new JavaPackage("org.sonar"), CoreMetrics.COVERAGE, 80.0);
    check("avoidConflictWithResourceFromOtherProject", "projects", "snapshots", "project_measures");
  }

  @Test
  public void doNotPersistInMemoryMeasures() throws ParseException {
    setup("doNotPersistInMemoryMeasures");
    Measure measure = new Measure(CoreMetrics.NCLOC, 30.0).setPersistenceMode(PersistenceMode.MEMORY);
    context.saveMeasure(measure);

    check("doNotPersistInMemoryMeasures", "projects", "snapshots", "project_measures");
    assertThat(context.getMeasure(CoreMetrics.NCLOC).getValue(), is(30.0));
  }

  @Test
  public void doNotCacheDatabaseMeasures() throws ParseException {
    setup("doNotCacheDatabaseMeasures");
    Measure measure = new Measure(CoreMetrics.NCLOC, 500.0).setPersistenceMode(PersistenceMode.DATABASE);
    context.saveMeasure(measure);

    check("doNotCacheDatabaseMeasures", "projects", "snapshots", "project_measures");
    assertThat(context.getMeasure(CoreMetrics.NCLOC), nullValue());
  }

  @Test
  public void saveRuleMeasures() throws ParseException {
    setup("saveRuleMeasures");
    context.saveMeasure(RuleMeasure.createForPriority(CoreMetrics.VIOLATIONS, RulePriority.CRITICAL, 500.0));
    context.saveMeasure(RuleMeasure.createForCategory(CoreMetrics.VIOLATIONS, 3, 200.0));
    //FIXME context.saveMeasure(RuleMeasure.createForRule(CoreMetrics.VIOLATIONS, 3).setIntValue(50.0));
    check("saveRuleMeasures", "projects", "snapshots", "project_measures");
  }

  @Test
  public void saveResourceTree() throws ParseException {
//    setup("saveResourceTree");
//
//    assertThat(context.getResource("org.foo.Bar"), nullValue());
//    context.saveResource(new JavaFile("org.foo.Bar"));
//    assertThat(context.getResource("org.foo.Bar"), is((Resource) new JavaFile("org.foo.Bar")));
//
//    check("saveResourceTree", "projects", "snapshots");
  }
//
//  @Test
//  public void doNotSaveExcludedResources() throws ParseException {
//    setup("doNotSaveExcludedResources");
//
//    JavaFile javaFile = new JavaFile("org.excluded.Bar");
//    ResourceFilters resourceFilters = mock(ResourceFilters.class);
//    when(resourceFilters.isExcluded(javaFile)).thenReturn(true);
//    context.setResourceFilters(resourceFilters);
//
//    assertThat(context.getResource("org.excluded.Bar"), nullValue());
//    assertThat(context.saveResource(javaFile), nullValue());
//    assertThat(context.getResource("org.excluded.Bar"), nullValue());
//
//    check("doNotSaveExcludedResources", "projects", "snapshots");
//  }

  @Test
  public void updateExistingResourceFields() throws ParseException {
    setup("updateExistingResourceFields");

    context.saveResource(new JavaPackage("org.foo"));

    check("updateExistingResourceFields", "projects", "snapshots");
  }

  @Test
  public void doNotSaveOptimizedBestValues() throws ParseException {
    setup("doNotSaveOptimizedBestValues");

    // best values of the metrics violations and blocker_violations are set as optimized
    assertThat(CoreMetrics.VIOLATIONS.getBestValue(), is(0.0));
    assertThat(CoreMetrics.BLOCKER_VIOLATIONS.getBestValue(), is(0.0));
    assertThat(CoreMetrics.VIOLATIONS.isOptimizedBestValue(), is(true));
    assertThat(CoreMetrics.BLOCKER_VIOLATIONS.isOptimizedBestValue(), is(true));

    final Resource javaFile = new JavaFile("org.foo.Bar");
    assertThat(context.getMeasure(javaFile, CoreMetrics.VIOLATIONS), nullValue());
    context.saveMeasure(javaFile, CoreMetrics.VIOLATIONS, 60.0); // saved
    assertThat(context.getMeasure(javaFile, CoreMetrics.VIOLATIONS).getValue(), is(60.0));

    assertThat(context.getMeasure(javaFile, CoreMetrics.BLOCKER_VIOLATIONS), nullValue());
    context.saveMeasure(javaFile, CoreMetrics.BLOCKER_VIOLATIONS, 0.0); // not saved in database
    assertThat(context.getMeasure(javaFile, CoreMetrics.BLOCKER_VIOLATIONS).getValue(), is(0.0));

    check("doNotSaveOptimizedBestValues", "projects", "snapshots", "project_measures");
  }

  @Test
  public void saveOptimizedBestValuesIfOptionalFields() throws ParseException {
    setup("saveOptimizedBestValuesIfOptionalFields");

    // best value of the metric violations is set as optimized
    assertThat(CoreMetrics.VIOLATIONS.getBestValue(), is(0.0));
    assertThat(CoreMetrics.VIOLATIONS.isOptimizedBestValue(), is(true));

    final Resource javaFile = new JavaFile("org.foo.Bar");
    assertThat(context.getMeasure(javaFile, CoreMetrics.VIOLATIONS), nullValue());
    Measure measure = new Measure(CoreMetrics.VIOLATIONS, 0.0).setTendency(1);

    context.saveMeasure(javaFile, measure); // saved

    assertThat(context.getMeasure(javaFile, CoreMetrics.VIOLATIONS).getValue(), is(0.0));
    assertThat(context.getMeasure(javaFile, CoreMetrics.VIOLATIONS).getTendency(), is(1));

    check("saveOptimizedBestValuesIfOptionalFields", "projects", "snapshots", "project_measures");
  }


  @Test
  public void saveDependency() throws ParseException {
    setup("saveDependency");

    JavaPackage pac1 = new JavaPackage("org.sonar.source");
    JavaPackage pac2 = new JavaPackage("org.sonar.target");
    context.saveResource(pac1);
    context.saveResource(pac2);

    Dependency dep = new Dependency(pac1, pac2)
        .setUsage("INHERITS")
        .setWeight(3);
    context.saveDependency(dep);

    assertThat(dep.getId(), not(nullValue()));

    check("saveDependency", "projects", "snapshots", "dependencies");
  }

  @Test(expected = IllegalArgumentException.class)
  public void saveResourcesBeforeBuildingDependencies() throws ParseException {
    setup("saveResourcesBeforeBuildingDependencies");

    JavaPackage pac1 = new JavaPackage("org.sonar.source");
    JavaPackage pac2 = new JavaPackage("org.sonar.target");
    context.saveDependency(new Dependency(pac1, pac2));
  }


  private void setup(String unitTest) throws ParseException {
//    setupData(unitTest);
//    project = mock(Project.class);
//    when(project.getAnalysisVersion()).thenReturn("1.0");
//    when(project.getAnalysisDate()).thenReturn(new SimpleDateFormat("yyyy-MM-dd").parse("2008-12-25"));
//    when(project.getKey()).thenReturn("group:artifact");
//    when(project.getScope()).thenReturn(Resource.SCOPE_SET);
//    when(project.getQualifier()).thenReturn(Resource.QUALIFIER_PROJECT);
//    when(project.getLanguage()).thenReturn(Java.INSTANCE);
//    when(project.getId()).thenReturn(10);
//    when(project.getName()).thenReturn("my project");
//    when(project.isRoot()).thenReturn(true);
//    ProjectBootstrap projectBootstrap = new ProjectBootstrap(null);
//    projectBootstrap.setProject(project);
//    projectBootstrap.setSnapshot(getSnapshot(1));
//    context = new DefaultSensorContext(getSession(), projectBootstrap.setProject(project), getDao().getMeasuresDao(), null, null, null);
  }

  private void check(String unitTest, String... tables) {
    getSession().commit();
    checkTables(unitTest, tables);
  }

  private Snapshot getSnapshot(int id) {
    Query query = getSession().createQuery("SELECT s FROM Snapshot s WHERE s.id=:id");
    query.setParameter("id", id);
    return (Snapshot) query.getSingleResult();
  }
}
