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
package org.sonar.xoo.rule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.config.Configuration;
import org.sonar.api.rule.RuleKey;

/**
 * Generate one issue per Xoo file with internal tags. There can be many tags, based on pattern recognition.
 * The pattern is [INTERNAL_TAG](tag_name).
 * Use the property sonar.enable.internalTags to enable/disable the addition of internal tags to issues. Enabled by default.
 */
public class InternalTagsIssueSensor extends AbstractXooRuleSensor {

  public static final String RULE_KEY = "InternalTagIssue";
  public static final Pattern INTERNAL_TAG_PATTERN =  Pattern.compile("\\[INTERNAL_TAG\\]\\(([a-zA-Z0-9_-]+)\\)");

  private static final String ENABLE_INTERNAL_TAGS_PROPERTY = "sonar.enable.internalTags";

  private final Configuration settings;

  public InternalTagsIssueSensor(Configuration settings, FileSystem fs, ActiveRules activeRules) {
    super(fs, activeRules);
    this.settings = settings;
  }

  @Override
  protected String getRuleKey() {
    return RULE_KEY;
  }

  @Override
  protected void processFile(InputFile inputFile, SensorContext context, RuleKey ruleKey, String languageKey) {
    boolean shouldAddTags = Boolean.parseBoolean(settings.get(ENABLE_INTERNAL_TAGS_PROPERTY).orElse("true"));
    List<String> internalTags = shouldAddTags ? getInternalTagsForFile(inputFile) : new ArrayList<>();
    NewIssue newIssue = context.newIssue()
      .forRule(ruleKey)
      .setInternalTags(internalTags);
    newIssue.at(newIssue.newLocation().on(inputFile).message("This issue can have internal tags."))
      .save();
  }

  private List<String> getInternalTagsForFile(InputFile inputFile) {
    try {
      String fileContents = inputFile.contents();
      Matcher matcher = INTERNAL_TAG_PATTERN.matcher(fileContents);
      List<String> internalTags = new ArrayList<>();
      while (matcher.find()) {
        internalTags.add(matcher.group(1));
      }
      return internalTags;
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read file", e);
    }
  }
}
