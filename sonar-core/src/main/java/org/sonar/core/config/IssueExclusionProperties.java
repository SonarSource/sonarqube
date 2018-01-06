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
package org.sonar.core.config;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sonar.api.CoreProperties;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyFieldDefinition;
import org.sonar.api.resources.Qualifiers;

public final class IssueExclusionProperties {

  public static final String SUB_CATEGORY_IGNORE_ISSUES = "issues";

  public static final String EXCLUSION_KEY_PREFIX = "sonar.issue.ignore";
  public static final String INCLUSION_KEY_PREFIX = "sonar.issue.enforce";

  public static final String MULTICRITERIA_SUFFIX = ".multicriteria";
  public static final String PATTERNS_MULTICRITERIA_EXCLUSION_KEY = EXCLUSION_KEY_PREFIX + MULTICRITERIA_SUFFIX;
  public static final String PATTERNS_MULTICRITERIA_INCLUSION_KEY = INCLUSION_KEY_PREFIX + MULTICRITERIA_SUFFIX;
  public static final String RESOURCE_KEY = "resourceKey";
  private static final String PROPERTY_FILE_PATH_PATTERN = "File Path Pattern";
  public static final String RULE_KEY = "ruleKey";
  private static final String PROPERTY_RULE_KEY_PATTERN = "Rule Key Pattern";
  private static final String PROPERTY_RULE_KEY_PATTERN_HELP = "<br/>A rule key pattern consists of the rule repository name, followed by a colon, followed by a rule key "
    + "or rule name fragment. For example:"
    + "<ul><li>squid:S1195</li><li>squid:*Naming*</li></ul>";

  public static final String BLOCK_SUFFIX = ".block";
  public static final String PATTERNS_BLOCK_KEY = EXCLUSION_KEY_PREFIX + BLOCK_SUFFIX;
  public static final String BEGIN_BLOCK_REGEXP = "beginBlockRegexp";
  public static final String END_BLOCK_REGEXP = "endBlockRegexp";

  public static final String ALLFILE_SUFFIX = ".allfile";
  public static final String PATTERNS_ALLFILE_KEY = EXCLUSION_KEY_PREFIX + ALLFILE_SUFFIX;
  public static final String FILE_REGEXP = "fileRegexp";

  public static final int LARGE_SIZE = 40;

  private IssueExclusionProperties() {
    // only static
  }

  public static List<PropertyDefinition> all() {
    return ImmutableList.of(
      PropertyDefinition.builder(PATTERNS_MULTICRITERIA_EXCLUSION_KEY)
        .category(CoreProperties.CATEGORY_EXCLUSIONS)
        .subCategory(SUB_CATEGORY_IGNORE_ISSUES)
        .name("Ignore Issues on Multiple Criteria")
        .description("Patterns to ignore issues on certain components and for certain coding rules." + PROPERTY_RULE_KEY_PATTERN_HELP)
        .onQualifiers(Qualifiers.PROJECT)
        .index(3)
        .fields(
          PropertyFieldDefinition.build(RULE_KEY)
            .name(PROPERTY_RULE_KEY_PATTERN)
            .description("Pattern to match rules which should be ignored.")
            .type(PropertyType.STRING)
            .indicativeSize(LARGE_SIZE)
            .build(),
          PropertyFieldDefinition.build(RESOURCE_KEY)
            .name(PROPERTY_FILE_PATH_PATTERN)
            .description("Pattern to match files which should be ignored.")
            .type(PropertyType.STRING)
            .indicativeSize(LARGE_SIZE)
            .build()
        )
        .build(),
      PropertyDefinition.builder(PATTERNS_BLOCK_KEY)
        .category(CoreProperties.CATEGORY_EXCLUSIONS)
        .subCategory(SUB_CATEGORY_IGNORE_ISSUES)
        .name("Ignore Issues in Blocks")
        .description("Patterns to ignore all issues (except the ones from the common repository) on specific blocks of code, " +
          "while continuing to scan and mark issues on the remainder of the file.")
        .onQualifiers(Qualifiers.PROJECT)
        .index(2)
        .fields(
          PropertyFieldDefinition.build(BEGIN_BLOCK_REGEXP)
            .name("Regular Expression for Start of Block")
            .description("If this regular expression is found in a file, then following lines are ignored until end of block.")
            .type(PropertyType.STRING)
            .indicativeSize(LARGE_SIZE)
            .build(),
          PropertyFieldDefinition.build(END_BLOCK_REGEXP)
            .name("Regular Expression for End of Block")
            .description("If specified, this regular expression is used to determine the end of code blocks to ignore. If not, then block ends at the end of file.")
            .type(PropertyType.STRING)
            .indicativeSize(LARGE_SIZE)
            .build()
        )
        .build(),
      PropertyDefinition.builder(PATTERNS_ALLFILE_KEY)
        .category(CoreProperties.CATEGORY_EXCLUSIONS)
        .subCategory(SUB_CATEGORY_IGNORE_ISSUES)
        .name("Ignore Issues on Files")
        .description("Patterns to ignore all issues (except the ones from the common repository) on files that contain a block of code matching a given regular expression.")
        .onQualifiers(Qualifiers.PROJECT)
        .index(1)
        .fields(
          PropertyFieldDefinition.build(FILE_REGEXP)
            .name("Regular Expression")
            .description("If this regular expression is found in a file, then the whole file is ignored.")
            .type(PropertyType.STRING)
            .indicativeSize(LARGE_SIZE)
            .build()
        )
        .build(),
      PropertyDefinition.builder(PATTERNS_MULTICRITERIA_INCLUSION_KEY)
        .category(CoreProperties.CATEGORY_EXCLUSIONS)
        .subCategory(SUB_CATEGORY_IGNORE_ISSUES)
        .name("Restrict Scope of Coding Rules")
        .description("Patterns to restrict the application of a rule to only certain components, ignoring all others." + PROPERTY_RULE_KEY_PATTERN_HELP)
        .onQualifiers(Qualifiers.PROJECT)
        .index(4)
        .fields(
          PropertyFieldDefinition.build(RULE_KEY)
            .name(PROPERTY_RULE_KEY_PATTERN)
            .description("Pattern used to match rules which should be restricted.")
            .type(PropertyType.STRING)
            .indicativeSize(LARGE_SIZE)
            .build(),
          PropertyFieldDefinition.build(RESOURCE_KEY)
            .name(PROPERTY_FILE_PATH_PATTERN)
            .description("Pattern used to match files to which the rules should be restricted.")
            .type(PropertyType.STRING)
            .indicativeSize(LARGE_SIZE)
            .build()
        )
        .build()
      );
  }
}
