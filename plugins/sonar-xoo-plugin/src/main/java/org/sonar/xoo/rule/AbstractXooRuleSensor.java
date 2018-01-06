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

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.rule.RuleKey;
import org.sonar.xoo.Xoo;

public abstract class AbstractXooRuleSensor implements Sensor {

  private final FileSystem fs;
  private final ActiveRules activeRules;

  public AbstractXooRuleSensor(FileSystem fs, ActiveRules activeRules) {
    this.fs = fs;
    this.activeRules = activeRules;
  }

  protected abstract String getRuleKey();

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .onlyOnLanguage(Xoo.KEY)
      .createIssuesForRuleRepository(XooRulesDefinition.XOO_REPOSITORY);
  }

  @Override
  public void execute(SensorContext context) {
    doAnalyse(context, Xoo.KEY);
  }

  private void doAnalyse(SensorContext context, String languageKey) {
    RuleKey ruleKey = RuleKey.of(XooRulesDefinition.XOO_REPOSITORY, getRuleKey());
    if (activeRules.find(ruleKey) == null) {
      return;
    }
    for (InputFile inputFile : fs.inputFiles(fs.predicates().hasLanguage(languageKey))) {
      processFile(inputFile, context, ruleKey, languageKey);
    }
  }

  protected abstract void processFile(InputFile inputFile, SensorContext context, RuleKey ruleKey, String languageKey);
}
