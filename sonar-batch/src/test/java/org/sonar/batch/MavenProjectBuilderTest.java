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

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.jpa.test.AbstractDbUnitTestCase;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MavenProjectBuilderTest extends AbstractDbUnitTestCase {

  private MavenProjectBuilder builder = null;

  @Before
  public void before() {
    builder = new MavenProjectBuilder(getSession());
  }

  @Test
  public void noExclusionPatterns() {
    Project project = new Project("key");
    builder.configure(project, new PropertiesConfiguration());

    assertThat(project.getExclusionPatterns().length, is(0));
  }

  @Test
  public void manyExclusionPatterns() {
    PropertiesConfiguration configuration = new PropertiesConfiguration();
    configuration.setProperty(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY, "**/*,foo,*/bar");

    Project project = new Project("key");
    builder.configure(project, configuration);

    assertThat(project.getExclusionPatterns().length, is(3));
    assertThat(project.getExclusionPatterns()[0], is("**/*"));
    assertThat(project.getExclusionPatterns()[1], is("foo"));
    assertThat(project.getExclusionPatterns()[2], is("*/bar"));
  }

  @Test
  public void getLanguageFromConfiguration() {
    PropertiesConfiguration configuration = new PropertiesConfiguration();
    configuration.setProperty(CoreProperties.PROJECT_LANGUAGE_PROPERTY, "foo");

    Project project = new Project("key");
    builder.configure(project, configuration);

    assertThat(project.getLanguageKey(), is("foo"));
  }

  @Test
  public void defaultLanguageIsJava() {
    Project project = new Project("key");
    builder.configure(project, new PropertiesConfiguration());

    assertThat(project.getLanguageKey(), is(Java.KEY));
  }

  @Test
  public void analysisIsTodayByDefault() {
    Project project = new Project("key");
    builder.configure(project, new PropertiesConfiguration());
    Date today = new Date();
    assertTrue(today.getTime() - project.getAnalysisDate().getTime() < 1000);
  }

  @Test
  public void analysisDateCouldBeExplicitlySet() {
    PropertiesConfiguration configuration = new PropertiesConfiguration();
    configuration.setProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2005-01-30");
    Project project = new Project("key");
    builder.configure(project, configuration);

    assertEquals("30012005", new SimpleDateFormat("ddMMyyyy").format(project.getAnalysisDate()));
  }

  @Test(expected = RuntimeException.class)
  public void failIfAnalyisDateIsNotValid() {
    PropertiesConfiguration configuration = new PropertiesConfiguration();
    configuration.setProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2005/30/01");
    Project project = new Project("key");
    builder.configure(project, configuration);

    project.getAnalysisDate();
  }

  @Test
  public void sonarLightIsDeprecated() {
    PropertiesConfiguration configuration = new PropertiesConfiguration();
    configuration.setProperty("sonar.light", "true");
    Project project = new Project("key");
    builder.configure(project, configuration);

    assertThat(project.getAnalysisType(), is(Project.AnalysisType.STATIC));
  }

  @Test
  public void defaultAnalysisTypeIsDynamic() {
    Project project = new Project("key");
    builder.configure(project, new PropertiesConfiguration());
    assertThat(project.getAnalysisType(), is(Project.AnalysisType.DYNAMIC));
  }

  @Test
  public void explicitDynamicAnalysis() {
    PropertiesConfiguration configuration = new PropertiesConfiguration();
    configuration.setProperty(CoreProperties.DYNAMIC_ANALYSIS_PROPERTY, "true");
    Project project = new Project("key");
    builder.configure(project, configuration);
    assertThat(project.getAnalysisType(), is(Project.AnalysisType.DYNAMIC));
  }

  @Test
  public void explicitStaticAnalysis() {
    PropertiesConfiguration configuration = new PropertiesConfiguration();
    configuration.setProperty(CoreProperties.DYNAMIC_ANALYSIS_PROPERTY, "false");
    Project project = new Project("key");
    builder.configure(project, configuration);
    assertThat(project.getAnalysisType(), is(Project.AnalysisType.STATIC));
  }

  @Test
  public void explicitDynamicAnalysisReusingReports() {
    PropertiesConfiguration configuration = new PropertiesConfiguration();
    configuration.setProperty(CoreProperties.DYNAMIC_ANALYSIS_PROPERTY, "reuseReports");
    Project project = new Project("key");
    builder.configure(project, configuration);
    assertThat(project.getAnalysisType(), is(Project.AnalysisType.REUSE_REPORTS));
  }

  @Test
  public void isDynamicAnalysis() {
    assertThat(Project.AnalysisType.DYNAMIC.isDynamic(false), is(true));
    assertThat(Project.AnalysisType.DYNAMIC.isDynamic(true), is(true));

    assertThat(Project.AnalysisType.STATIC.isDynamic(false), is(false));
    assertThat(Project.AnalysisType.STATIC.isDynamic(true), is(false));

    assertThat(Project.AnalysisType.REUSE_REPORTS.isDynamic(false), is(false));
    assertThat(Project.AnalysisType.REUSE_REPORTS.isDynamic(true), is(true));
  }

  @Test
  public void isLatestAnalysis() {
    setupData("isLatestAnalysis");

    PropertiesConfiguration configuration = new PropertiesConfiguration();
    configuration.setProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2010-12-25");

    Project project = new Project("my:key");
    builder.configure(project, configuration);

    assertThat(project.isLatestAnalysis(), is(true));
  }

  @Test
  public void isLatestAnalysisIfNeverAnalysed() {
    setupData("isLatestAnalysisIfNeverAnalysed");

    PropertiesConfiguration configuration = new PropertiesConfiguration();
    configuration.setProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2010-12-25");

    Project project = new Project("my:key");
    builder.configure(project, configuration);

    assertThat(project.isLatestAnalysis(), is(true));
  }

  @Test
  public void isNotLatestAnalysis() {
    setupData("isNotLatestAnalysis");

    PropertiesConfiguration configuration = new PropertiesConfiguration();
    configuration.setProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2005-12-25");

    Project project = new Project("my:key");
    builder.configure(project, configuration);

    assertThat(project.isLatestAnalysis(), is(false));
  }
}
