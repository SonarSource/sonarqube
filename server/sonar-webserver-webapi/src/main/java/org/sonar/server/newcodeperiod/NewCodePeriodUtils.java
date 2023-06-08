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
package org.sonar.server.newcodeperiod;

import com.google.common.base.Preconditions;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.newcodeperiod.NewCodePeriodParser;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.exceptions.NotFoundException;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.NUMBER_OF_DAYS;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.PREVIOUS_VERSION;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.REFERENCE_BRANCH;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.SPECIFIC_ANALYSIS;

public interface NewCodePeriodUtils {
  String BEGIN_LIST = "<ul>";

  String END_LIST = "</ul>";
  String BEGIN_ITEM_LIST = "<li>";
  String END_ITEM_LIST = "</li>";
  String NEW_CODE_PERIOD_TYPE_DESCRIPTION = "Type<br/>" +
    "New code definitions of the following types are allowed:" +
    BEGIN_LIST +
    BEGIN_ITEM_LIST + SPECIFIC_ANALYSIS.name() + " - can be set at branch level only" + END_ITEM_LIST +
    BEGIN_ITEM_LIST + PREVIOUS_VERSION.name() + " - can be set at any level (global, project, branch)" + END_ITEM_LIST +
    BEGIN_ITEM_LIST + NUMBER_OF_DAYS.name() + " - can be set at any level (global, project, branch)" + END_ITEM_LIST +
    BEGIN_ITEM_LIST + REFERENCE_BRANCH.name() + " - can only be set for projects and branches" + END_ITEM_LIST +
    END_LIST;

  String NEW_CODE_PERIOD_TYPE_DESCRIPTION_PROJECT_CREATION = "Type<br/>" +
    "New code definitions of the following types are allowed:" +
    BEGIN_LIST +
    BEGIN_ITEM_LIST + PREVIOUS_VERSION.name() + END_ITEM_LIST +
    BEGIN_ITEM_LIST + NUMBER_OF_DAYS.name() + END_ITEM_LIST +
    BEGIN_ITEM_LIST + REFERENCE_BRANCH.name() + " - will default to the main branch. A new code definition should be set for the branch itself. " +
    "Until you do so, this branch will use itself as a reference branch and no code will be considered new for this branch" + END_ITEM_LIST +
    END_LIST;

  String NEW_CODE_PERIOD_VALUE_DESCRIPTION = "Value<br/>" +
    "For each type, a different value is expected:" +
    BEGIN_LIST +
    BEGIN_ITEM_LIST + "the uuid of an analysis, when type is " + SPECIFIC_ANALYSIS.name() + END_ITEM_LIST +
    BEGIN_ITEM_LIST + "no value, when type is " + PREVIOUS_VERSION.name() + END_ITEM_LIST +
    BEGIN_ITEM_LIST + "a number between 1 and 90, when type is " + NUMBER_OF_DAYS.name() + END_ITEM_LIST +
    BEGIN_ITEM_LIST + "a string, when type is " + REFERENCE_BRANCH.name() + END_ITEM_LIST +
    END_LIST;
  String NEW_CODE_PERIOD_VALUE_DESCRIPTION_PROJECT_CREATION  = "Value<br/>" +
    "For each type, a different value is expected:" +
    BEGIN_LIST +
    BEGIN_ITEM_LIST + "no value, when type is " + PREVIOUS_VERSION.name() + " and " + REFERENCE_BRANCH.name() + END_ITEM_LIST +
    BEGIN_ITEM_LIST + "a number between 1 and 90, when type is " + NUMBER_OF_DAYS.name() + END_ITEM_LIST +
    END_LIST;

  String UNEXPECTED_VALUE_ERROR_MESSAGE = "Unexpected value for type '%s'";

  static Optional<String> getNewCodeDefinitionValue(DbSession dbSession, DbClient dbClient, NewCodePeriodType type, @Nullable ProjectDto project,
    @Nullable BranchDto branch, @Nullable String value) {
    switch (type) {
      case PREVIOUS_VERSION:
        Preconditions.checkArgument(value == null, UNEXPECTED_VALUE_ERROR_MESSAGE, type);
        return Optional.empty();
      case NUMBER_OF_DAYS:
        requireValue(type, value);
        return Optional.of(parseDays(value));
      case SPECIFIC_ANALYSIS:
        requireValue(type, value);
        requireBranch(type, branch);
        SnapshotDto analysis = getAnalysis(dbSession, value, project, branch, dbClient);
        return Optional.of(analysis.getUuid());
      case REFERENCE_BRANCH:
        requireValue(type, value);
        return Optional.of(value);
      default:
        throw new IllegalStateException("Unexpected type: " + type);
    }
  }

  static Optional<String> getNewCodeDefinitionValueProjectCreation(NewCodePeriodType type, @Nullable String value, String defaultBranchName) {
    switch (type) {
      case PREVIOUS_VERSION:
        Preconditions.checkArgument(value == null, UNEXPECTED_VALUE_ERROR_MESSAGE, type);
        return Optional.empty();
      case NUMBER_OF_DAYS:
        requireValue(type, value);
        return Optional.of(parseDays(value));
      case REFERENCE_BRANCH:
        Preconditions.checkArgument(value == null, UNEXPECTED_VALUE_ERROR_MESSAGE, type);
        return Optional.of(defaultBranchName);
      default:
        throw new IllegalStateException("Unexpected type: " + type);
    }
  }

  private static String parseDays(String value) {
    try {
      return Integer.toString(NewCodePeriodParser.parseDays(value));
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to parse number of days: " + value);
    }
  }

  private static void requireValue(NewCodePeriodType type, @Nullable String value) {
    Preconditions.checkArgument(value != null, "New code definition type '%s' requires a value", type);
  }

  private static void requireBranch(NewCodePeriodType type, @Nullable BranchDto branch) {
    Preconditions.checkArgument(branch != null, "New code definition type '%s' requires a branch", type);
  }
  private static Set<NewCodePeriodType> getInstanceTypes() {
    return EnumSet.of(PREVIOUS_VERSION, NUMBER_OF_DAYS);
  }

  private static Set<NewCodePeriodType> getProjectTypes() {
    return EnumSet.of(PREVIOUS_VERSION, NUMBER_OF_DAYS, REFERENCE_BRANCH);
  }

  private static Set<NewCodePeriodType> getBranchTypes() {
    return EnumSet.of(PREVIOUS_VERSION, NUMBER_OF_DAYS, SPECIFIC_ANALYSIS, REFERENCE_BRANCH);
  }

  static NewCodePeriodType validateType(String typeStr, boolean isOverall, boolean isBranch) {
    NewCodePeriodType type;
    try {
      type = NewCodePeriodType.valueOf(typeStr.toUpperCase(Locale.US));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid type: " + typeStr);
    }

    if (isOverall) {
      checkType("Overall setting", getInstanceTypes(), type);
    } else if (isBranch) {
      checkType("Branches", getBranchTypes(), type);
    } else {
      checkType("Projects", getProjectTypes(), type);
    }
    return type;
  }

  private static SnapshotDto getAnalysis(DbSession dbSession, String analysisUuid, ProjectDto project, BranchDto branch, DbClient dbClient) {
    SnapshotDto snapshotDto = dbClient.snapshotDao().selectByUuid(dbSession, analysisUuid)
      .orElseThrow(() -> new NotFoundException(format("Analysis '%s' is not found", analysisUuid)));
    checkAnalysis(dbSession, project, branch, snapshotDto, dbClient);
    return snapshotDto;
  }

  private static void checkAnalysis(DbSession dbSession, ProjectDto project, BranchDto branch, SnapshotDto analysis, DbClient dbClient) {
    BranchDto analysisBranch = dbClient.branchDao().selectByUuid(dbSession, analysis.getComponentUuid()).orElse(null);
    boolean analysisMatchesProjectBranch = analysisBranch != null && analysisBranch.getUuid().equals(branch.getUuid());

    checkArgument(analysisMatchesProjectBranch,
      "Analysis '%s' does not belong to branch '%s' of project '%s'",
      analysis.getUuid(), branch.getKey(), project.getKey());
  }

  private static void checkType(String name, Set<NewCodePeriodType> validTypes, NewCodePeriodType type) {
    if (!validTypes.contains(type)) {
      throw new IllegalArgumentException(String.format("Invalid type '%s'. %s can only be set with types: %s", type, name, validTypes));
    }
  }

}
