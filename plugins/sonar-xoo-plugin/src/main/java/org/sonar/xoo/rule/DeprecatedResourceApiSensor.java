/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.xoo.rule;

import java.io.File;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.xoo.Xoo;

@SuppressWarnings("deprecation")
public class DeprecatedResourceApiSensor implements Sensor {

  public static final String RULE_KEY = "DeprecatedResourceApi";
  private final FileSystem fs;
  private final ResourcePerspectives perspectives;
  private final ActiveRules activeRules;

  public DeprecatedResourceApiSensor(FileSystem fileSystem, ResourcePerspectives perspectives, ActiveRules activeRules) {
    this.fs = fileSystem;
    this.perspectives = perspectives;
    this.activeRules = activeRules;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return fs.hasFiles(fs.predicates().and(fs.predicates().hasType(Type.MAIN), fs.predicates().hasLanguage(Xoo.KEY)))
      && activeRules.find(RuleKey.of(XooRulesDefinition.XOO_REPOSITORY, RULE_KEY)) != null;
  }

  @Override
  public void analyse(Project module, org.sonar.api.batch.SensorContext context) {
    for (File f : fs.files(fs.predicates().and(fs.predicates().hasType(Type.MAIN), fs.predicates().hasLanguage(Xoo.KEY)))) {
      String relativePathFromBaseDir = new PathResolver().relativePath(fs.baseDir(), f);
      org.sonar.api.resources.File sonarFile = org.sonar.api.resources.File.create(relativePathFromBaseDir);
      Issuable issuable = perspectives.as(Issuable.class, sonarFile);
      issuable.addIssue(issuable.newIssueBuilder()
        .ruleKey(RuleKey.of(XooRulesDefinition.XOO_REPOSITORY, RULE_KEY))
        .message("Issue created using deprecated API")
        .line(1)
        .build());

      // Message and line are nullable
      issuable.addIssue(issuable.newIssueBuilder()
        .ruleKey(RuleKey.of(XooRulesDefinition.XOO_REPOSITORY, RULE_KEY))
        .message(null)
        .line(null)
        .build());

      sonarFile = context.getResource(sonarFile);
      Directory parent = sonarFile.getParent();
      createIssueOnDir(parent);
    }

  }

  private Directory createIssueOnDir(Directory dir) {
    Issuable issuable = perspectives.as(Issuable.class, dir);
    issuable.addIssue(issuable.newIssueBuilder()
      .ruleKey(RuleKey.of(XooRulesDefinition.XOO_REPOSITORY, RULE_KEY))
      .message("Issue created using deprecated API")
      .build());
    return dir;
  }

}
