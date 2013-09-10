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

public interface Constants {
  // New Properties
  String SUB_CATEGORY_IGNORE_ISSUES = "Ignore Issues";

  String CORE_KEY_PREFIX = "sonar.issue.ignore";

  String MULTICRITERIA_SUFFIX = ".multicriteria";
  String PATTERNS_MULTICRITERIA_KEY = CORE_KEY_PREFIX + MULTICRITERIA_SUFFIX;
  String RESOURCE_KEY = "resourceKey";
  String RULE_KEY = "ruleKey";
  String LINE_RANGE_KEY = "lineRange";

  String BLOCK_SUFFIX = ".block";
  String PATTERNS_BLOCK_KEY = CORE_KEY_PREFIX + BLOCK_SUFFIX;
  String BEGIN_BLOCK_REGEXP = "beginBlockRegexp";
  String END_BLOCK_REGEXP = "endBlockRegexp";

  String ALLFILE_SUFFIX = ".allfile";
  String PATTERNS_ALLFILE_KEY = CORE_KEY_PREFIX + ALLFILE_SUFFIX;
  String FILE_REGEXP = "fileRegexp";
}
