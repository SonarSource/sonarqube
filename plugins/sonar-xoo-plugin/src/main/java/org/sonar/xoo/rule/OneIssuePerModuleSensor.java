/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.xoo.rule;

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.xoo.Xoo;

public class OneIssuePerModuleSensor implements Sensor {

  public static final String RULE_KEY = "OneIssuePerModule";

  private final ResourcePerspectives perspectives;
  private final FileSystem fs;
  private final ActiveRules activeRules;

  public OneIssuePerModuleSensor(ResourcePerspectives perspectives, FileSystem fs, ActiveRules activeRules) {
    this.perspectives = perspectives;
    this.fs = fs;
    this.activeRules = activeRules;
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    Issuable issuable = perspectives.as(Issuable.class, project);
    issuable.addIssue(issuable.newIssueBuilder()
      .ruleKey(RuleKey.of(XooRulesDefinition.XOO_REPOSITORY, RULE_KEY))
      .message("This issue is generated on each module")
      .build());
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return fs.hasFiles(fs.predicates().hasLanguages(Xoo.KEY)) && (activeRules.find(RuleKey.of(XooRulesDefinition.XOO_REPOSITORY, RULE_KEY)) != null);
  }
}
