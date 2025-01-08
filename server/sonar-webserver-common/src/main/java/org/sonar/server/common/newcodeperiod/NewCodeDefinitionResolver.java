/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.common.newcodeperiod;

import com.google.common.base.Preconditions;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.db.newcodeperiod.NewCodePeriodParser;
import org.sonar.db.newcodeperiod.NewCodePeriodType;

import static org.sonar.db.newcodeperiod.NewCodePeriodType.NUMBER_OF_DAYS;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.PREVIOUS_VERSION;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.REFERENCE_BRANCH;

public class NewCodeDefinitionResolver {
  private static final String BEGIN_LIST = "<ul>";

  private static final String END_LIST = "</ul>";
  private static final String BEGIN_ITEM_LIST = "<li>";
  private static final String END_ITEM_LIST = "</li>";

  public static final String NEW_CODE_PERIOD_TYPE_DESCRIPTION_PROJECT_CREATION = "Project New Code Definition Type<br/>" +
    "New code definitions of the following types are allowed:" +
    BEGIN_LIST +
    BEGIN_ITEM_LIST + PREVIOUS_VERSION.name() + END_ITEM_LIST +
    BEGIN_ITEM_LIST + NUMBER_OF_DAYS.name() + END_ITEM_LIST +
    BEGIN_ITEM_LIST + REFERENCE_BRANCH.name() + " - will default to the main branch." + END_ITEM_LIST +
    END_LIST;

  public static final String NEW_CODE_PERIOD_VALUE_DESCRIPTION_PROJECT_CREATION = "Project New Code Definition Value<br/>" +
    "For each new code definition type, a different value is expected:" +
    BEGIN_LIST +
    BEGIN_ITEM_LIST + "no value, when the new code definition type is " + PREVIOUS_VERSION.name() + " and " + REFERENCE_BRANCH.name() + END_ITEM_LIST +
    BEGIN_ITEM_LIST + "a number between 1 and 90, when the new code definition type is " + NUMBER_OF_DAYS.name() + END_ITEM_LIST +
    END_LIST;

  private static final String UNEXPECTED_VALUE_ERROR_MESSAGE = "Unexpected value for newCodeDefinitionType '%s'";

  private static final EnumSet<NewCodePeriodType> projectCreationNCDTypes = EnumSet.of(PREVIOUS_VERSION, NUMBER_OF_DAYS, REFERENCE_BRANCH);

  private final DbClient dbClient;
  private final PlatformEditionProvider editionProvider;

  public NewCodeDefinitionResolver(DbClient dbClient, PlatformEditionProvider editionProvider) {
    this.dbClient = dbClient;
    this.editionProvider = editionProvider;
  }

  public void createNewCodeDefinition(DbSession dbSession, String projectUuid, String mainBranchUuid,
    String defaultBranchName, String newCodeDefinitionType, @Nullable String newCodeDefinitionValue) {

    boolean isCommunityEdition = editionProvider.get().filter(EditionProvider.Edition.COMMUNITY::equals).isPresent();
    NewCodePeriodType newCodePeriodType = parseNewCodeDefinitionType(newCodeDefinitionType);

    NewCodePeriodDto dto = new NewCodePeriodDto();
    dto.setType(newCodePeriodType);
    dto.setProjectUuid(projectUuid);

    if (isCommunityEdition) {
      dto.setBranchUuid(mainBranchUuid);
    }

    getNewCodeDefinitionValueProjectCreation(newCodePeriodType, newCodeDefinitionValue, defaultBranchName).ifPresent(dto::setValue);

    if (!CaycUtils.isNewCodePeriodCompliant(dto.getType(), dto.getValue())) {
      throw new IllegalArgumentException("Failed to set the New Code Definition. The given value is not compatible with the Clean as You Code methodology. "
        + "Please refer to the documentation for compliant options.");
    }

    dbClient.newCodePeriodDao().insert(dbSession, dto);
  }

  public static void checkNewCodeDefinitionParam(@Nullable String newCodeDefinitionType, @Nullable String newCodeDefinitionValue) {
    if (newCodeDefinitionType == null && newCodeDefinitionValue != null) {
      throw new IllegalArgumentException("New code definition type is required when new code definition value is provided");
    }
  }

  private static Optional<String> getNewCodeDefinitionValueProjectCreation(NewCodePeriodType type, @Nullable String value, String defaultBranchName) {
    return switch (type) {
      case PREVIOUS_VERSION -> {
        Preconditions.checkArgument(value == null, UNEXPECTED_VALUE_ERROR_MESSAGE, type);
        yield Optional.empty();
      }
      case NUMBER_OF_DAYS -> {
        requireValue(type, value);
        yield Optional.of(parseDays(value));
      }
      case REFERENCE_BRANCH -> {
        Preconditions.checkArgument(value == null, UNEXPECTED_VALUE_ERROR_MESSAGE, type);
        yield Optional.of(defaultBranchName);
      }
      default -> throw new IllegalStateException("Unexpected type: " + type);
    };
  }

  private static String parseDays(String value) {
    try {
      return Integer.toString(NewCodePeriodParser.parseDays(value));
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to parse number of days: " + value);
    }
  }

  private static void requireValue(NewCodePeriodType type, @Nullable String value) {
    Preconditions.checkArgument(value != null, "New code definition type '%s' requires a newCodeDefinitionValue", type);
  }

  private static NewCodePeriodType parseNewCodeDefinitionType(String typeStr) {
    NewCodePeriodType type;
    try {
      type = NewCodePeriodType.valueOf(typeStr.toUpperCase(Locale.US));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid type: " + typeStr);
    }
    validateType(type);
    return type;
  }

  private static void validateType(NewCodePeriodType type) {
    Preconditions.checkArgument(projectCreationNCDTypes.contains(type), "Invalid type '%s'. `newCodeDefinitionType` can only be set with types: %s",
      type, projectCreationNCDTypes);
  }

}
