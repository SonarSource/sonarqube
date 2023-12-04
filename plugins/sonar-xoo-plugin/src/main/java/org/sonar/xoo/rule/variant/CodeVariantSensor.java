/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.xoo.rule.variant;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.config.Configuration;
import org.sonar.api.rule.RuleKey;
import org.sonar.xoo.rule.AbstractXooRuleSensor;

/**
 * Raise issue for multiple code variants.
 * Use the property "sonar.variants" to define the variants.
 * If variant names are found on the file content, an issue is raised with all the corresponding variants.
 * Extend this abstract class to define the rule key.
 */
public abstract class CodeVariantSensor extends AbstractXooRuleSensor {

  private static final String VARIANTS_PROPERTY = "sonar.variants";

  private final Configuration settings;

  public CodeVariantSensor(Configuration settings, FileSystem fs, ActiveRules activeRules) {
    super(fs, activeRules);
    this.settings = settings;
  }

  @Override
  protected void processFile(InputFile inputFile, SensorContext context, RuleKey ruleKey, String languageKey) {
    Optional<String> variantsValue = settings.get(VARIANTS_PROPERTY);
    if (variantsValue.isEmpty()) {
      return;
    }

    List<String> variants = Arrays.asList(variantsValue.get().split(","));

    try {
      String contents = inputFile.contents();
      List<String> identifiedVariants = variants.stream()
        .filter(contents::contains)
        .toList();

      if (!identifiedVariants.isEmpty()) {
        NewIssue newIssue = context.newIssue()
          .forRule(ruleKey)
          .setCodeVariants(identifiedVariants);
        newIssue.at(newIssue.newLocation()
            .on(inputFile)
            .message("This is generated for variants"))
          .save();
      }
    } catch (IOException e) {
      throw new IllegalStateException("Fail to get content of file " + inputFile, e);
    }
  }

}
