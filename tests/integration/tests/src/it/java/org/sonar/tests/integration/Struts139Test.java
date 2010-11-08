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
package org.sonar.tests.integration;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.*;

import java.util.Date;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparisons.greaterThan;
import static org.hamcrest.number.OrderingComparisons.greaterThanOrEqualTo;
import static org.hamcrest.number.OrderingComparisons.lessThan;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * To execute these unit tests, just check-out Struts 1.3.9 from subversion:
 * http://svn.apache.org/repos/asf/struts/struts1/tags/STRUTS_1_3_9
 * <p/>
 * The quality profile to use is not important.
 */
public class Struts139Test {

  private static Sonar sonar;
  private static final String PROJECT_STRUTS = "org.apache.struts:struts-parent";
  private static final String MODULE_CORE = "org.apache.struts:struts-core";
  private static final String PACKAGE_ACTION = "org.apache.struts:struts-core:org.apache.struts.action";
  private static final String FILE_ACTION = "org.apache.struts:struts-core:org.apache.struts.action.Action";

  @BeforeClass
  public static void buildServer() {
    sonar = Sonar.create("http://localhost:9000");
  }

  @Test
  public void shouldReturnAnalysisDate() {
    Date date = sonar.find(new ResourceQuery(PROJECT_STRUTS)).getDate();
    assertNotNull(date);
    assertThat(date.getYear(), greaterThanOrEqualTo(110)); // 1900 + 110
  }

  @Test
  public void strutsIsAnalyzed() {
    assertThat(sonar.find(new ResourceQuery(PROJECT_STRUTS)).getName(), is("Struts"));
    assertThat(sonar.find(new ResourceQuery(PROJECT_STRUTS)).getVersion(), is("1.3.9"));
    assertThat(sonar.find(new ResourceQuery(MODULE_CORE)).getName(), is("Struts Core"));
  }

  @Test
  public void sizeMetrics() {
    assertThat(getProjectMeasure("lines").getIntValue(), is(114621));
    assertThat(getProjectMeasure("ncloc").getIntValue(), is(50080));
    assertThat(getProjectMeasure("functions").getIntValue(), is(4292));
    assertThat(getProjectMeasure("accessors").getIntValue(), is(1133));
    assertThat(getProjectMeasure("classes").getIntValue(), is(518));
    assertThat(getProjectMeasure("packages").getIntValue(), is(49));
    assertThat(getProjectMeasure("files").getIntValue(), is(494));
    assertThat(getCoreModuleMeasure("files").getIntValue(), is(134));
    assertThat(getPackageMeasure("files").getIntValue(), is(21));
    assertThat(getFileMeasure("files").getIntValue(), is(1));
  }

  @Test
  public void unitTestMetrics() {
    assertThat(getProjectMeasure("coverage").getValue(), is(14.7));
    assertThat(getProjectMeasure("line_coverage").getValue(), is(15.4));
    assertThat(getProjectMeasure("branch_coverage").getValue(), is(12.8));
    assertThat(getProjectMeasure("tests").getIntValue(), is(323));
    assertThat(getProjectMeasure("test_execution_time").getIntValue(), greaterThan(1000));
    assertThat(getProjectMeasure("test_errors").getIntValue(), is(0));
    assertThat(getProjectMeasure("test_failures").getIntValue(), is(0));
    assertThat(getProjectMeasure("skipped_tests").getIntValue(), is(0));
    assertThat(getProjectMeasure("test_success_density").getValue(), is(100.0));

  }

  @Test
  public void complexityMetrics() {
    assertThat(getProjectMeasure("complexity").getIntValue(), is(11140));
    assertThat(getProjectMeasure("statements").getIntValue(), is(21896));
    assertThat(getProjectMeasure("class_complexity").getValue(), is(21.5));
    assertThat(getProjectMeasure("function_complexity").getValue(), is(2.6));
    assertThat(getProjectMeasure("class_complexity_distribution").getData(), is("0=172;5=90;10=86;20=55;30=69;60=34;90=17"));
  }

  @Test
  public void lcom4() {
    assertThat(getProjectMeasure("lcom4").getValue(), greaterThan(1.5));
    assertThat(getProjectMeasure("lcom4").getValue(), lessThan(2.0));
    assertThat(getFileMeasure("lcom4").getValue(), greaterThan(10.0));
  }

  @Test
  public void rfc() {
    assertThat(getProjectMeasure("rfc").getValue(), greaterThan(10.0));
    assertThat(getProjectMeasure("rfc").getValue(), lessThan(30.0));
  }

  @Test
  public void design() {
    assertThat(getCoreModuleMeasure("package_cycles").getIntValue(), greaterThan(10));
    assertThat(getCoreModuleMeasure("package_cycles").getIntValue(), lessThan(50));
    
    assertThat(getCoreModuleMeasure("package_feedback_edges").getIntValue(), greaterThan(3));
    assertThat(getCoreModuleMeasure("package_feedback_edges").getIntValue(), lessThan(10));

    assertThat(getCoreModuleMeasure("package_tangles").getIntValue(), greaterThan(10));
    assertThat(getCoreModuleMeasure("package_tangles").getIntValue(), lessThan(50));

    assertThat(sonar.find(ResourceQuery.createForMetrics(PROJECT_STRUTS, "dit", "noc")).getMeasures().size(), is(0));
  }

  @Test
  public void dependencyTree() {
    List<DependencyTree> trees = sonar.findAll(DependencyTreeQuery.createForProject(PROJECT_STRUTS));
    assertThat(trees.size(), is(0));

    trees = sonar.findAll(DependencyTreeQuery.createForProject(MODULE_CORE));
    assertThat(trees.size(), greaterThan(0));
    assertThat(trees.get(0).getResourceName(), is("antlr:antlr"));
  }

  @Test
  public void versionEvent() {
    EventQuery query = new EventQuery(PROJECT_STRUTS);
    query.setCategories(new String[]{"Version"});
    List<Event> events = sonar.findAll(query);
    assertThat(events.size(), is(1));

    Event version = events.get(0);
    assertThat(version.getName(), is("1.3.9"));
    assertThat(version.getCategory(), is("Version"));
  }

  private Measure getFileMeasure(String metricKey) {
    return sonar.find(ResourceQuery.createForMetrics(FILE_ACTION, metricKey)).getMeasure(metricKey);
  }

  private Measure getCoreModuleMeasure(String metricKey) {
    return sonar.find(ResourceQuery.createForMetrics(MODULE_CORE, metricKey)).getMeasure(metricKey);
  }

  private Measure getProjectMeasure(String metricKey) {
    return sonar.find(ResourceQuery.createForMetrics(PROJECT_STRUTS, metricKey)).getMeasure(metricKey);
  }
  
  private Measure getPackageMeasure(String metricKey) {
    return sonar.find(ResourceQuery.createForMetrics(PACKAGE_ACTION, metricKey)).getMeasure(metricKey);
  }
}
