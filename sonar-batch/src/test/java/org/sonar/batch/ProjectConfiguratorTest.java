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
package org.sonar.batch;

import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProjectConfiguratorTest extends AbstractDbUnitTestCase {

  @Test
  public void analysisIsTodayByDefault() {
    Project project = new Project("key");
    new ProjectConfigurator(getSession(), new Settings()).configure(project);
    Date today = new Date();
    assertTrue(today.getTime() - project.getAnalysisDate().getTime() < 1000);
  }

  @Test
  public void analysisDateCouldBeExplicitlySet() {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2005-01-30");
    Project project = new Project("key");
    new ProjectConfigurator(getSession(), settings).configure(project);

    assertEquals("30012005", new SimpleDateFormat("ddMMyyyy").format(project.getAnalysisDate()));
  }

  @Test
  public void analysisTimestampCouldBeExplicitlySet() {
    Settings settings = new Settings();
    settings.setProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2005-01-30T08:45:10+0000");
    Project project = new Project("key");
    new ProjectConfigurator(getSession(), settings).configure(project);

    assertEquals("30012005-4510", new SimpleDateFormat("ddMMyyyy-mmss").format(project.getAnalysisDate()));
  }

  @Test(expected = RuntimeException.class)
  public void failIfAnalyisDateIsNotValid() {
    Settings configuration = new Settings();
    configuration.setProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2005/30/01");
    Project project = new Project("key");
    new ProjectConfigurator(getSession(), configuration).configure(project);
  }

  @Test
  public void defaultAnalysisTypeIsDynamic() {
    Project project = new Project("key");
    new ProjectConfigurator(getSession(), new Settings()).configure(project);
    assertThat(project.getAnalysisType(), is(Project.AnalysisType.DYNAMIC));
  }

  @Test
  public void explicitDynamicAnalysis() {
    Settings configuration = new Settings();
    configuration.setProperty(CoreProperties.DYNAMIC_ANALYSIS_PROPERTY, "true");
    Project project = new Project("key");
    new ProjectConfigurator(getSession(), configuration).configure(project);
    assertThat(project.getAnalysisType(), is(Project.AnalysisType.DYNAMIC));
  }

  @Test
  public void explicitStaticAnalysis() {
    Settings configuration = new Settings();
    configuration.setProperty(CoreProperties.DYNAMIC_ANALYSIS_PROPERTY, "false");
    Project project = new Project("key");
    new ProjectConfigurator(getSession(), configuration).configure(project);
    assertThat(project.getAnalysisType(), is(Project.AnalysisType.STATIC));
  }

  @Test
  public void explicitDynamicAnalysisReusingReports() {
    Settings configuration = new Settings();
    configuration.setProperty(CoreProperties.DYNAMIC_ANALYSIS_PROPERTY, "reuseReports");
    Project project = new Project("key");
    new ProjectConfigurator(getSession(), configuration).configure(project);
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

    Settings configuration = new Settings();
    configuration.setProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2010-12-25");

    Project project = new Project("my:key");
    new ProjectConfigurator(getSession(), configuration).configure(project);

    assertThat(project.isLatestAnalysis(), is(true));
  }

  @Test
  public void isLatestAnalysisIfNeverAnalysed() {
    setupData("isLatestAnalysisIfNeverAnalysed");

    Settings configuration = new Settings();
    configuration.setProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2010-12-25");

    Project project = new Project("my:key");
    new ProjectConfigurator(getSession(), configuration).configure(project);

    assertThat(project.isLatestAnalysis(), is(true));
  }

  @Test
  public void isNotLatestAnalysis() {
    setupData("isNotLatestAnalysis");

    Settings configuration = new Settings();
    configuration.setProperty(CoreProperties.PROJECT_DATE_PROPERTY, "2005-12-25");

    Project project = new Project("my:key");
    new ProjectConfigurator(getSession(), configuration).configure(project);

    assertThat(project.isLatestAnalysis(), is(false));
  }
}
