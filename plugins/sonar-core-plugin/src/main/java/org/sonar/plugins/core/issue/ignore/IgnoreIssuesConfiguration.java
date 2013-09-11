/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.plugins.core.issue.ignore;

import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyFieldDefinition;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.CoreProperties;
import com.google.common.collect.ImmutableList;
import org.sonar.api.config.PropertyDefinition;

import java.util.List;

public final class IgnoreIssuesConfiguration {

  public static final String CONFIG_DOCUMENTATION_LINK = "More information on the "
    + "<a href=\"http://docs.codehaus.org/display/SONAR/Project+Administration#ProjectAdministration-IgnoringIssues\">Project Administration page</a>.<br/>";

  public static final String SUB_CATEGORY_IGNORE_ISSUES = "issues";

  public static final String CORE_KEY_PREFIX = "sonar.issue.ignore";

  public static final String MULTICRITERIA_SUFFIX = ".multicriteria";
  public static final String PATTERNS_MULTICRITERIA_KEY = CORE_KEY_PREFIX + MULTICRITERIA_SUFFIX;
  public static final String RESOURCE_KEY = "resourceKey";
  public static final String RULE_KEY = "ruleKey";
  public static final String LINE_RANGE_KEY = "lineRange";

  public static final String BLOCK_SUFFIX = ".block";
  public static final String PATTERNS_BLOCK_KEY = CORE_KEY_PREFIX + BLOCK_SUFFIX;
  public static final String BEGIN_BLOCK_REGEXP = "beginBlockRegexp";
  public static final String END_BLOCK_REGEXP = "endBlockRegexp";

  public static final String ALLFILE_SUFFIX = ".allfile";
  public static final String PATTERNS_ALLFILE_KEY = CORE_KEY_PREFIX + ALLFILE_SUFFIX;
  public static final String FILE_REGEXP = "fileRegexp";

  private IgnoreIssuesConfiguration() {
    // static configuration declaration only
  }

  static final int LARGE_SIZE = 20;
  static final int SMALL_SIZE = 10;

  public static List<PropertyDefinition> getPropertyDefinitions() {
    return ImmutableList.of(
      PropertyDefinition.builder(PATTERNS_MULTICRITERIA_KEY)
        .category(CoreProperties.CATEGORY_EXCLUSIONS)
        .subCategory(SUB_CATEGORY_IGNORE_ISSUES)
        .name("File Path Pattern")
        .description("Patterns used to identify which violations to switch off.<br/>" +
          CONFIG_DOCUMENTATION_LINK)
        .onQualifiers(Qualifiers.PROJECT)
        .index(3)
        .fields(
          PropertyFieldDefinition.build(RESOURCE_KEY)
            .name("File Path Pattern")
            .description("Pattern used to match files which should be ignored")
            .type(PropertyType.STRING)
            .indicativeSize(LARGE_SIZE)
            .build(),
          PropertyFieldDefinition.build(RULE_KEY)
            .name("Rule Key Pattern")
            .description("Pattern used to match rules which should be ignored")
            .type(PropertyType.STRING)
            .indicativeSize(LARGE_SIZE)
            .build(),
          PropertyFieldDefinition.build(LINE_RANGE_KEY)
            .name("Line Range")
            .description("Range of lines that should be ignored.")
            .type(PropertyType.STRING)
            .indicativeSize(SMALL_SIZE)
            .build())
        .build(),
        PropertyDefinition.builder(PATTERNS_BLOCK_KEY)
        .category(CoreProperties.CATEGORY_EXCLUSIONS)
        .subCategory(SUB_CATEGORY_IGNORE_ISSUES)
        .name("Block exclusion patterns")
        .description("Patterns used to identify blocks in which violations are switched off.<br/>" +
          CONFIG_DOCUMENTATION_LINK)
        .onQualifiers(Qualifiers.PROJECT)
        .index(2)
        .fields(
          PropertyFieldDefinition.build(BEGIN_BLOCK_REGEXP)
            .name("Regular expression for start of block")
            .description("If this regular expression is found in a file, then following lines are ignored until end of block.")
            .type(PropertyType.STRING)
            .indicativeSize(LARGE_SIZE)
            .build(),
          PropertyFieldDefinition.build(END_BLOCK_REGEXP)
            .name("Regular expression for end of block")
            .description("If specified, this regular expression is used to determine the end of code blocks to ignore. If not, then block ends at the end of file.")
            .type(PropertyType.STRING)
            .indicativeSize(LARGE_SIZE)
            .build())
        .build(),
        PropertyDefinition.builder(PATTERNS_ALLFILE_KEY)
        .category(CoreProperties.CATEGORY_EXCLUSIONS)
        .subCategory(SUB_CATEGORY_IGNORE_ISSUES)
        .name("File exclusion patterns")
        .description("Patterns used to identify files in which violations are switched off.<br/>" +
          CONFIG_DOCUMENTATION_LINK)
        .onQualifiers(Qualifiers.PROJECT)
        .index(1)
        .fields(
          PropertyFieldDefinition.build(FILE_REGEXP)
            .name("Regular expression")
            .description("If this regular expression is found in a file, then following lines are ignored.")
            .type(PropertyType.STRING)
            .indicativeSize(LARGE_SIZE)
            .build())
        .build());
  }
}
