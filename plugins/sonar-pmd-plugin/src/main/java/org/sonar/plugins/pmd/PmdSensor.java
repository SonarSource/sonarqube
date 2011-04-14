/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import java.io.File;

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.Logs;
import org.sonar.api.utils.XmlParserException;

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
      File xmlReport = executor.execute();
      getStaxParser(project, context).parse(xmlReport);

    } catch (Exception e) {
      // TOFIX
      throw new XmlParserException(e);
    }
  }

  public boolean shouldExecuteOnProject(Project project) {
    return project.getFileSystem().hasJavaSourceFiles() &&
        !profile.getActiveRulesByRepository(PmdConstants.REPOSITORY_KEY).isEmpty();
  }

  private PmdViolationsXmlParser getStaxParser(Project project, SensorContext context) {
    return new PmdViolationsXmlParser(project, rulesFinder, context);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
