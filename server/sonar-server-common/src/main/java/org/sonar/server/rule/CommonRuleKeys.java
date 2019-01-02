/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.rule;

public class CommonRuleKeys {

  public static final String REPOSITORY_PREFIX = "common-";

  public static final String INSUFFICIENT_BRANCH_COVERAGE = "InsufficientBranchCoverage";
  public static final String INSUFFICIENT_BRANCH_COVERAGE_PROPERTY = "minimumBranchCoverageRatio";

  public static final String INSUFFICIENT_LINE_COVERAGE = "InsufficientLineCoverage";
  public static final String INSUFFICIENT_LINE_COVERAGE_PROPERTY = "minimumLineCoverageRatio";

  public static final String INSUFFICIENT_COMMENT_DENSITY = "InsufficientCommentDensity";
  public static final String INSUFFICIENT_COMMENT_DENSITY_PROPERTY = "minimumCommentDensity";

  public static final String DUPLICATED_BLOCKS = "DuplicatedBlocks";
  public static final String FAILED_UNIT_TESTS = "FailedUnitTests";
  public static final String SKIPPED_UNIT_TESTS = "SkippedUnitTests";

  private CommonRuleKeys() {
    // only static methods
  }

  public static String commonRepositoryForLang(String language) {
    return REPOSITORY_PREFIX + language;
  }
}
