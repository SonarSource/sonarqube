/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.xoo.rule.features;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.xoo.rule.AbstractXooRuleSensor;

/**
 * Generate issues based on feature availability checks.
 * Based on pattern recognition: [REQUIRED_FEATURE](feature_name).
 * Issues are raised only when the specified feature is available.
 */
public class AvailableFeatureSensor extends AbstractXooRuleSensor {

  public static final String RULE_KEY = "AvailableFeature";
  public static final Pattern REQUIRED_FEATURE_PATTERN = Pattern.compile("\\[REQUIRED_FEATURE\\]\\(([a-zA-Z0-9_-]+)\\)");

  public AvailableFeatureSensor(FileSystem fs, ActiveRules activeRules) {
    super(fs, activeRules);
  }

  @Override
  protected String getRuleKey() {
    return RULE_KEY;
  }

  @Override
  protected void processFile(InputFile inputFile, SensorContext context, RuleKey ruleKey, String languageKey) {
    try {
      String fileContents = inputFile.contents();
      Matcher matcher = REQUIRED_FEATURE_PATTERN.matcher(fileContents);

      while (matcher.find()) {
        String featureName = matcher.group(1);
        if (context.isFeatureAvailable(featureName)) {
          NewIssue newIssue = context.newIssue()
            .forRule(ruleKey);

          int lineNumber = getLineNumber(fileContents, matcher.start());
          newIssue.at(newIssue.newLocation()
            .on(inputFile)
            .at(inputFile.selectLine(lineNumber))
            .message("Issue raised because feature '" + featureName + "' is available"))
            .save();
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read file", e);
    }
  }

  private int getLineNumber(String content, int position) {
    int lineNumber = 1;
    for (int i = 0; i < position; i++) {
      if (content.charAt(i) == '\n') {
        lineNumber++;
      }
    }
    return lineNumber;
  }
}
