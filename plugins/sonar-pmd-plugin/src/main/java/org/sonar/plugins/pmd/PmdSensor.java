/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.plugins.pmd;

import net.sourceforge.pmd.IRuleViolation;
import net.sourceforge.pmd.Report;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.XmlParserException;

import java.util.Iterator;

public class PmdSensor implements Sensor {

  private RulesProfile profile;
  private RuleFinder rulesFinder;
  private PmdExecutor executor;

  public PmdSensor(RulesProfile profile, RuleFinder rulesFinder, PmdExecutor executor) {
    this.profile = profile;
    this.rulesFinder = rulesFinder;
    this.executor = executor;
  }

  public void analyse(Project project, SensorContext context) {
    try {
      Report report = executor.execute();
      analyseReport(report, project, context);

    } catch (Exception e) {
      // TOFIX
      throw new XmlParserException(e);
    }
  }

  private void analyseReport(Report report, Project project, SensorContext context) {
    Iterator<IRuleViolation> pmdViolationIter = report.iterator();
    while (pmdViolationIter.hasNext()) {
      IRuleViolation pmdViolation = pmdViolationIter.next();
      int lineId = pmdViolation.getBeginLine();
      String ruleKey = pmdViolation.getRule().getName();
      String message = pmdViolation.getDescription();
      String filename = pmdViolation.getFilename();
      Resource resource = JavaFile.fromAbsolutePath(filename, project.getFileSystem().getSourceDirs(), false);
      // Save violations only for existing resources
      if (context.getResource(resource) != null) {
        Rule rule = rulesFinder.findByKey(CoreProperties.PMD_PLUGIN, ruleKey);
        // Save violations only for enabled rules
        if (rule != null) {
          Violation violation = Violation.create(rule, resource).setLineId(lineId).setMessage(message);
          context.saveViolation(violation);
        }
      }
    }
  }

  public boolean shouldExecuteOnProject(Project project) {
    return !project.getFileSystem().mainFiles(Java.KEY).isEmpty() &&
      !profile.getActiveRulesByRepository(PmdConstants.REPOSITORY_KEY).isEmpty();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
