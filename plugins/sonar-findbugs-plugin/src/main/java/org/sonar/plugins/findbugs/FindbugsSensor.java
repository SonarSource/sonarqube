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
package org.sonar.plugins.findbugs;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.Violation;

public class FindbugsSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(FindbugsSensor.class);

  private RulesProfile profile;
  private RuleFinder ruleFinder;
  private FindbugsExecutor executor;

  public FindbugsSensor(RulesProfile profile, RuleFinder ruleFinder, FindbugsExecutor executor) {
    this.profile = profile;
    this.ruleFinder = ruleFinder;
    this.executor = executor;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return Java.KEY.equals(project.getLanguageKey())
        && !project.getFileSystem().mainFiles(Java.KEY).isEmpty()
        && !profile.getActiveRulesByRepository(FindbugsConstants.REPOSITORY_KEY).isEmpty();
  }

  public void analyse(Project project, SensorContext context) {
    if (project.getReuseExistingRulesConfig()) {
      LOG.warn("Reusing existing Findbugs configuration not supported any more.");
    }

    BugCollection collection = executor.execute();

    for (BugInstance bugInstance : collection) {
      SourceLineAnnotation sourceLine = bugInstance.getPrimarySourceLineAnnotation();
      if (sourceLine == null) {
        LOG.warn("No source line for " + bugInstance.getType());
        continue;
      }

      Rule rule = ruleFinder.findByKey(FindbugsConstants.REPOSITORY_KEY, bugInstance.getType());
      if (rule == null) {
        // ignore violations from report, if rule not activated in Sonar
        LOG.warn("Findbugs rule '{}' not active in Sonar.", bugInstance.getType());
        continue;
      }

      String longMessage = bugInstance.getMessageWithoutPrefix();
      String className = bugInstance.getPrimarySourceLineAnnotation().getClassName();
      int start = bugInstance.getPrimarySourceLineAnnotation().getStartLine();

      JavaFile resource = new JavaFile(getSonarJavaFileKey(className));
      if (context.getResource(resource) != null) {
        Violation violation = Violation.create(rule, resource)
            .setLineId(start)
            .setMessage(longMessage);
        context.saveViolation(violation);
      }
    }
  }

  private static String getSonarJavaFileKey(String className) {
    if (className.indexOf('$') > -1) {
      return className.substring(0, className.indexOf('$'));
    }
    return className;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
