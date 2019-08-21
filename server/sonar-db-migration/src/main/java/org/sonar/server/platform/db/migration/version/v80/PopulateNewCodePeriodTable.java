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
package org.sonar.server.platform.db.migration.version.v80;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.Upsert;

public class PopulateNewCodePeriodTable extends DataChange {
  private static final Logger LOG = Loggers.get(PopulateNewCodePeriodTable.class);

  private static final String TYPE_PREVIOUS_VERSION = "PREVIOUS_VERSION";
  private static final String TYPE_NUMBER_OF_DAYS = "NUMBER_OF_DAYS";
  private static final String TYPE_SPECIFIC_ANALYSIS = "SPECIFIC_ANALYSIS";

  private static final String LEAK_PERIOD_PROP_KEY = "sonar.leak.period";

  private final UuidFactory uuidFactory;
  private final System2 system2;

  public PopulateNewCodePeriodTable(Database db, UuidFactory uuidFactory, System2 system2) {
    super(db);
    this.uuidFactory = uuidFactory;
    this.system2 = system2;
  }

  @Override
  protected void execute(Context context) throws SQLException {
    List<String> branchUuidsAlreadyMigrated = populateFromManualBaselines(context);
    populateFromSettings(context, branchUuidsAlreadyMigrated);
  }

  private List<String> populateFromManualBaselines(Context context) throws SQLException {
    List<ProjectBranchManualBaselineDto> projectBranchManualBaselines = context.prepareSelect(
      "SELECT pb.uuid, pb.project_uuid, pb.manual_baseline_analysis_uuid FROM project_branches pb WHERE pb.manual_baseline_analysis_uuid IS NOT NULL")
      .list(row -> {
        String branchUuid = row.getString(1);
        String projectUuid = row.getString(2);
        String manualBaselineAnalysisUuid = row.getString(3);
        return new ProjectBranchManualBaselineDto(branchUuid, projectUuid, manualBaselineAnalysisUuid);
      });

    if (!projectBranchManualBaselines.isEmpty()) {
      populateWithManualBaselines(context, projectBranchManualBaselines);
    }

    return projectBranchManualBaselines.stream()
      .map(pb -> pb.branchUuid)
      .collect(Collectors.toList());
  }

  private void populateFromSettings(Context context, List<String> branchUuidsAlreadyMigrated) throws SQLException {
    // migrate global setting
    String globalSetting = context
      .prepareSelect("SELECT props.text_value FROM properties props WHERE props.prop_key = '" + LEAK_PERIOD_PROP_KEY + "' AND props.resource_id IS NULL")
      .get(r -> r.getString(1));
    if (globalSetting != null) {
      populateGlobalSetting(context, globalSetting);
    }

    List<BranchLeakPeriodPropertyDto> projectLeakPeriodProperties = new ArrayList<>();
    context.prepareSelect(
      "SELECT projs.uuid, projs.main_branch_project_uuid, props.text_value "
        + "FROM properties props INNER JOIN projects projs ON props.resource_id = projs.id "
        + "WHERE props.prop_key = '" + LEAK_PERIOD_PROP_KEY + "' AND props.resource_id IS NOT NULL ")
      .scroll(row -> {
        String branchUuid = row.getString(1);
        if (branchUuidsAlreadyMigrated.contains(branchUuid)) {
          return;
        }
        String mainBranchUuid = row.getString(2);
        String value = row.getString(3);
        if (mainBranchUuid == null) {
          mainBranchUuid = branchUuid;
        }
        projectLeakPeriodProperties.add(new BranchLeakPeriodPropertyDto(branchUuid, mainBranchUuid, value));
      });

    populateWithSettings(context, projectLeakPeriodProperties);
  }

  private void populateGlobalSetting(Context context, String value) {
    Optional<TypeValuePair> typeValue = tryParse(context, null, value);
    typeValue.ifPresent(tp -> {
      try (Upsert upsertQuery = prepareUpsertNewCodePeriodQuery(context)) {
        long currentTime = system2.now();
        insert(upsertQuery, null, null, tp.type, tp.value, currentTime);
        upsertQuery
          .execute()
          .commit();
      } catch (SQLException e) {
        LOG.warn("Failed to migrate the global property for the new code period", e);
      }
    });
  }

  private void populateWithSettings(Context context, List<BranchLeakPeriodPropertyDto> projectLeakPeriodProperties) throws SQLException {
    try (Upsert upsertQuery = prepareUpsertNewCodePeriodQuery(context)) {
      long currentTime = system2.now();
      boolean commit = false;

      for (BranchLeakPeriodPropertyDto branchLeakPeriodProperty : projectLeakPeriodProperties) {
        Optional<TypeValuePair> typeValueOpt = tryParse(context, branchLeakPeriodProperty.branchUuid, branchLeakPeriodProperty.value);
        if (!typeValueOpt.isPresent()) {
          continue;
        }

        TypeValuePair typeValue = typeValueOpt.get();

        String branchUuid = null;
        if (!branchLeakPeriodProperty.isMainBranch() || TYPE_SPECIFIC_ANALYSIS.equals(typeValue.type)) {
          branchUuid = branchLeakPeriodProperty.branchUuid;
        }

        insert(upsertQuery, branchLeakPeriodProperty.mainBranchUuid, branchUuid, typeValue.type, typeValue.value, currentTime);
        commit = true;
      }

      if (commit) {
        upsertQuery
          .execute()
          .commit();
      }
    }
  }

  private void populateWithManualBaselines(Context context, List<ProjectBranchManualBaselineDto> projectBranchManualBaselines) throws SQLException {
    try (Upsert upsertQuery = prepareUpsertNewCodePeriodQuery(context)) {
      long currentTime = system2.now();

      for (ProjectBranchManualBaselineDto projectBranchManualBaseline : projectBranchManualBaselines) {
        insert(upsertQuery, projectBranchManualBaseline.projectUuid, projectBranchManualBaseline.branchUuid, TYPE_SPECIFIC_ANALYSIS,
          projectBranchManualBaseline.manualBaselineAnalysisUuid, currentTime);
      }
      upsertQuery
        .execute()
        .commit();
    }
  }

  private void insert(Upsert upsert, @Nullable String projectUuid, @Nullable String branchUuid, String type, @Nullable String value, long currentTime) throws SQLException {
    upsert
      .setString(1, uuidFactory.create())
      .setString(2, projectUuid)
      .setString(3, branchUuid)
      .setString(4, type)
      .setString(5, value)
      .setLong(6, currentTime)
      .setLong(7, currentTime)
      .addBatch();
  }

  private static Upsert prepareUpsertNewCodePeriodQuery(Context context) throws SQLException {
    return context.prepareUpsert("INSERT INTO new_code_periods(" +
      "uuid, " +
      "project_uuid," +
      "branch_uuid," +
      "type," +
      "value," +
      "updated_at," +
      "created_at" +
      ") VALUES (?, ?, ?, ?, ?, ?, ?)");
  }

  private static Optional<TypeValuePair> tryParse(Context context, @Nullable String branchUuid, String value) {
    try {
      if (value.equals("previous_version")) {
        return Optional.of(new TypeValuePair(TYPE_PREVIOUS_VERSION, null));
      }

      try {
        Integer.parseInt(value);
        return Optional.of(new TypeValuePair(TYPE_NUMBER_OF_DAYS, value));
      } catch (NumberFormatException e) {
        // ignore
      }

      if (branchUuid == null) {
        return Optional.empty();
      }

      try {
        LocalDate localDate = LocalDate.parse(value);
        Optional<String> snapshot = findFirstSnapshot(context, branchUuid, localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
        return snapshot.map(uuid -> new TypeValuePair(TYPE_SPECIFIC_ANALYSIS, uuid));
      } catch (DateTimeParseException e) {
        // ignore
      }

      List<EventDto> versions = getVersionsByMostRecentFirst(context, branchUuid);
      Optional<EventDto> versionMatch = versions.stream().filter(v -> v.version.equals(value)).findFirst();
      return versionMatch.map(e -> new TypeValuePair(TYPE_SPECIFIC_ANALYSIS, e.analysisUuid));
    } catch (SQLException e) {
      LOG.warn("Failed to migrate a property for the new code period", e);
      return Optional.empty();
    }
  }

  private static List<EventDto> getVersionsByMostRecentFirst(Context context, String branchUuid) throws SQLException {
    return context.prepareSelect("SELECT name, analysis_uuid FROM events "
      + "WHERE component_uuid = '" + branchUuid + "' AND category = 'Version' "
      + "ORDER BY created_at DESC")
      .list(r -> new EventDto(r.getString(1), r.getString(2)));
  }

  private static Optional<String> findFirstSnapshot(Context context, String branchUuid, long date) throws SQLException {
    String analysisUuid = context.prepareSelect("SELECT uuid FROM snapshots "
      + "WHERE component_uuid = '" + branchUuid + "' AND created_at > " + date + " AND status = 'P' "
      + "ORDER BY created_at ASC")
      .get(r -> r.getString(1));
    return Optional.ofNullable(analysisUuid);
  }

  private static class TypeValuePair {
    private final String type;
    @Nullable
    private final String value;

    private TypeValuePair(String type, @Nullable String value) {
      this.type = type;
      this.value = value;
    }
  }

  private static class EventDto {
    private final String version;
    private final String analysisUuid;

    private EventDto(String version, String analysisUuid) {
      this.version = version;
      this.analysisUuid = analysisUuid;
    }
  }

  private static class ProjectBranchManualBaselineDto {
    private final String branchUuid;
    private final String projectUuid;
    private final String manualBaselineAnalysisUuid;

    ProjectBranchManualBaselineDto(String branchUuid, String projectUuid, String manualBaselineAnalysisUuid) {
      this.branchUuid = branchUuid;
      this.projectUuid = projectUuid;
      this.manualBaselineAnalysisUuid = manualBaselineAnalysisUuid;
    }
  }

  private static class BranchLeakPeriodPropertyDto {
    private final String branchUuid;
    private final String mainBranchUuid;
    private final String value;

    BranchLeakPeriodPropertyDto(String branchUuid, String mainBranchUuid, String value) {
      this.branchUuid = branchUuid;
      this.mainBranchUuid = mainBranchUuid;
      this.value = value;
    }

    private boolean isMainBranch() {
      return mainBranchUuid.equals(branchUuid);
    }
  }
}
