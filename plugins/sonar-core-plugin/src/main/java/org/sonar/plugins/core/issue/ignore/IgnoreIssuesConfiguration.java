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

  private IgnoreIssuesConfiguration() {}

  static final int LARGE_SIZE = 20;
  static final int SMALL_SIZE = 10;

  public static List<PropertyDefinition> getPropertyDefinitions() {
    return ImmutableList.of(
      PropertyDefinition.builder(Constants.PATTERNS_MULTICRITERIA_KEY)
        .category(CoreProperties.CATEGORY_EXCLUSIONS)
        .subCategory(Constants.SUB_CATEGORY_IGNORE_ISSUES)
        .deprecatedKey(Constants.DEPRECATED_MULTICRITERIA_KEY)
        .name("Resource Key Pattern")
        .description("Patterns used to identify which violations to switch off.<br/>" +
          "More information on the <a href=\"http://docs.codehaus.org/display/SONAR/Project+Administration#ProjectAdministration-IgnoringIssues\">Project Administration page</a>.<br/>")
        .onQualifiers(Qualifiers.PROJECT)
        .fields(
          PropertyFieldDefinition.build(Constants.RESOURCE_KEY)
            .name("Resource Key Pattern")
            .description("Pattern used to match resources which should be ignored")
            .type(PropertyType.STRING)
            .indicativeSize(LARGE_SIZE)
            .build(),
          PropertyFieldDefinition.build(Constants.RULE_KEY)
            .name("Rule Key Pattern")
            .description("Pattern used to match rules which should be ignored")
            .type(PropertyType.STRING)
            .indicativeSize(LARGE_SIZE)
            .build(),
          PropertyFieldDefinition.build(Constants.LINE_RANGE_KEY)
            .name("Line Range")
            .description("Range of lines that should be ignored.")
            .type(PropertyType.STRING)
            .indicativeSize(SMALL_SIZE)
            .build())
        .build(),
        PropertyDefinition.builder(Constants.PATTERNS_BLOCK_KEY)
        .category(CoreProperties.CATEGORY_EXCLUSIONS)
        .subCategory(Constants.SUB_CATEGORY_IGNORE_ISSUES)
        .deprecatedKey(Constants.DEPRECATED_BLOCK_KEY)
        .name("Block exclusion patterns")
        .description("Patterns used to identify blocks in which violations are switched off.<br/>" +
          "More information on the <a href=\"http://docs.codehaus.org/display/SONAR/Project+Administration#ProjectAdministration-IgnoringIssues\">Project Administration page</a>.<br/>")
        .onQualifiers(Qualifiers.PROJECT)
        .fields(
          PropertyFieldDefinition.build(Constants.BEGIN_BLOCK_REGEXP)
            .name("Regular expression for start of block")
            .description("If this regular expression is found in a resource, then following lines are ignored until end of block.")
            .type(PropertyType.STRING)
            .indicativeSize(LARGE_SIZE)
            .build(),
          PropertyFieldDefinition.build(Constants.END_BLOCK_REGEXP)
            .name("Regular expression for end of block")
            .description("If specified, this regular expression is used to determine the end of code blocks to ignore. If not, then block ends at the end of file.")
            .type(PropertyType.STRING)
            .indicativeSize(LARGE_SIZE)
            .build())
        .build(),
        PropertyDefinition.builder(Constants.PATTERNS_ALLFILE_KEY)
        .category(CoreProperties.CATEGORY_EXCLUSIONS)
        .subCategory(Constants.SUB_CATEGORY_IGNORE_ISSUES)
        .deprecatedKey(Constants.DEPRECATED_ALLFILE_KEY)
        .name("File exclusion patterns")
        .description("Patterns used to identify files in which violations are switched off.<br/>" +
          "More information on the <a href=\"http://docs.codehaus.org/display/SONAR/Project+Administration#ProjectAdministration-IgnoringIssues\">Project Administration page</a>.<br/>")
        .onQualifiers(Qualifiers.PROJECT)
        .fields(
          PropertyFieldDefinition.build(Constants.FILE_REGEXP)
            .name("Regular expression")
            .description("If this regular expression is found in a resource, then following lines are ignored.")
            .type(PropertyType.STRING)
            .indicativeSize(LARGE_SIZE)
            .build())
        .build());
  }
}
