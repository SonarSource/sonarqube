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

import java.io.File;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.xoo.Xoo;

@SuppressWarnings("deprecation")
public class DeprecatedResourceApiSensor implements Sensor {

  public static final String RULE_KEY = "DeprecatedResourceApi";
  private final ModuleFileSystem fileSystem;
  private final ResourcePerspectives perspectives;
  private final ActiveRules activeRules;

  public DeprecatedResourceApiSensor(ModuleFileSystem fileSystem, ResourcePerspectives perspectives, ActiveRules activeRules) {
    this.fileSystem = fileSystem;
    this.perspectives = perspectives;
    this.activeRules = activeRules;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return !fileSystem.files(FileQuery.onMain().onLanguage(Xoo.KEY)).isEmpty() && activeRules.find(RuleKey.of(XooRulesDefinition.XOO_REPOSITORY, RULE_KEY)) != null;
  }

  @Override
  public void analyse(Project module, org.sonar.api.batch.SensorContext context) {
    createIssueOnDir(new Directory(""));
    File src = fileSystem.sourceDirs().get(0);

    for (File f : fileSystem.files(FileQuery.onMain().onLanguage(Xoo.KEY))) {
      String relativePathFromSourceDir = new PathResolver().relativePath(src, f);
      org.sonar.api.resources.File sonarFile = new org.sonar.api.resources.File(relativePathFromSourceDir);
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
