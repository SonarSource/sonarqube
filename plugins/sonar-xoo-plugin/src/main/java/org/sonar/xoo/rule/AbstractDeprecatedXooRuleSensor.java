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

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.xoo.Xoo;

/**
 * @deprecated to be replaced by {@link org.sonar.api.batch.sensor.Sensor}
 */
public abstract class AbstractDeprecatedXooRuleSensor implements Sensor {

  private final FileSystem fs;
  private final ActiveRules activeRules;

  public AbstractDeprecatedXooRuleSensor(FileSystem fs, ActiveRules activeRules) {
    this.fs = fs;
    this.activeRules = activeRules;
  }

  protected abstract String getRuleKey();

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return fs.hasFiles(fs.predicates().hasLanguages(Xoo.KEY))
      && (activeRules.find(RuleKey.of(XooRulesDefinition.XOO_REPOSITORY, getRuleKey())) != null);
  }

  @Override
  public final void analyse(Project project, SensorContext context) {
    doAnalyse(context, Xoo.KEY);
  }

  private void doAnalyse(SensorContext context, String languageKey) {
    RuleKey ruleKey = RuleKey.of(XooRulesDefinition.XOO_REPOSITORY, getRuleKey());
    if (activeRules.find(ruleKey) == null) {
      return;
    }
    for (InputFile inputFile : fs.inputFiles(fs.predicates().hasLanguage(languageKey))) {
      File sonarFile = File.create(inputFile.relativePath());
      sonarFile = context.getResource(sonarFile);
      processFile(inputFile, sonarFile, context, ruleKey, languageKey);
    }
  }

  protected abstract void processFile(InputFile inputFile, File sonarFile, SensorContext context, RuleKey ruleKey, String languageKey);
}
