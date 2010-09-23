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
package org.sonar.plugins.findbugs;

import java.io.File;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.GeneratesViolations;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.maven.DependsUponMavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.Violation;

public class FindbugsSensor implements Sensor, DependsUponMavenPlugin, GeneratesViolations {

  private RulesProfile profile;
  private RuleFinder ruleFinder;
  private FindbugsMavenPluginHandler pluginHandler;
  private static Logger LOG = LoggerFactory.getLogger(FindbugsSensor.class);

  public FindbugsSensor(RulesProfile profile, RuleFinder ruleFinder, FindbugsMavenPluginHandler pluginHandler) {
    this.profile = profile;
    this.ruleFinder = ruleFinder;
    this.pluginHandler = pluginHandler;
  }

  public void analyse(Project project, SensorContext context) {
    File report = getFindbugsReportFile(project);
    LOG.info("Findbugs output report: " + report.getAbsolutePath());
    FindbugsXmlReportParser reportParser = new FindbugsXmlReportParser(report);
    List<FindbugsXmlReportParser.Violation> fbViolations = reportParser.getViolations();
    for (FindbugsXmlReportParser.Violation fbViolation : fbViolations) {
      Rule rule = ruleFinder.findByKey(FindbugsConstants.REPOSITORY_KEY, fbViolation.getType());
      JavaFile resource = new JavaFile(fbViolation.getSonarJavaFileKey());
      if (context.getResource(resource) != null) {
        Violation violation = Violation.create(rule, resource).setLineId(fbViolation.getStart()).setMessage(fbViolation.getLongMessage());
        context.saveViolation(violation);
      }
    }
  }

  protected final File getFindbugsReportFile(Project project) {
    if (project.getConfiguration().getString(CoreProperties.FINDBUGS_REPORT_PATH) != null) {
      return new File(project.getConfiguration().getString(CoreProperties.FINDBUGS_REPORT_PATH));
    }
    return new File(project.getFileSystem().getBuildDir(), "findbugsXml.xml");
  }

  public boolean shouldExecuteOnProject(Project project) {
    return project.getFileSystem().hasJavaSourceFiles()
        && ( !profile.getActiveRulesByRepository(FindbugsConstants.REPOSITORY_KEY).isEmpty() || project.getReuseExistingRulesConfig())
        && project.getPom() != null && !StringUtils.equalsIgnoreCase(project.getPom().getPackaging(), "ear");
  }

  public MavenPluginHandler getMavenPluginHandler(Project project) {
    if (project.getConfiguration().getString(CoreProperties.FINDBUGS_REPORT_PATH) != null) {
      return null;
    }
    return pluginHandler;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}