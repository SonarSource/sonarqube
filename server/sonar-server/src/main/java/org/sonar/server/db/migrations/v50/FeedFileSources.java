/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.server.db.migrations.v50;

import static com.google.common.base.Charsets.UTF_8;

import java.sql.SQLException;
import java.util.Date;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.BaseDataChange;
import org.sonar.server.db.migrations.MassUpdate;
import org.sonar.server.db.migrations.Select.Row;
import org.sonar.server.db.migrations.Select.RowReader;
import org.sonar.server.db.migrations.SqlStatement;

/**
 * Used in the Active Record Migration 714
 *
 * @since 5.0
 */
public class FeedFileSources extends BaseDataChange {

  private static final String SELECT_FILES_AND_MEASURES_SQL = "SELECT " +
    "p.uuid, " +
    "f.uuid, " +
    "ss.data, " +
    "ss.updated_at, " +

    // revisions_by_line
    "m1.text_value, " +
    "m1.measure_data, " +

    // authors_by_line
    "m2.text_value, " +
    "m2.measure_data, " +

    // dates_by_line
    "m3.text_value, " +
    "m3.measure_data, " +

    // hits_by_line
    "m4.text_value, " +
    "m4.measure_data, " +

    // cond_by_line
    "m5.text_value, " +
    "m5.measure_data, " +

    // cover_cond_by_line
    "m6.text_value, " +
    "m6.measure_data, " +

    // it_hits_by_line
    "m7.text_value, " +
    "m7.measure_data, " +

    // it_cond_by_line
    "m8.text_value, " +
    "m8.measure_data, " +

    // it_cover_cond_by_line
    "m9.text_value, " +
    "m9.measure_data, " +

    // overall_hits_by_line
    "m10.text_value, " +
    "m10.measure_data, " +

    // overall_cond_by_line
    "m11.text_value, " +
    "m11.measure_data, " +

    // overall_cover_cond_by_line
    "m12.text_value, " +
    "m12.measure_data,  " +

    // duplication_data
    "m13.text_value, " +
    "m13.measure_data  " +

    "FROM snapshots s " +
    "JOIN snapshot_sources ss " +
    "ON s.id = ss.snapshot_id AND s.islast = ? " +
    "JOIN projects p " +
    "ON s.root_project_id = p.id " +
    "JOIN projects f " +
    "ON s.project_id = f.id " +
    "LEFT JOIN file_sources fs " +
    "ON fs.file_uuid = f.uuid " +
    "LEFT JOIN project_measures m1 " +
    "ON m1.snapshot_id = s.id AND m1.metric_id = ? " +
    "LEFT JOIN project_measures m2 " +
    "ON m2.snapshot_id = s.id AND m2.metric_id = ? " +
    "LEFT JOIN project_measures m3 " +
    "ON m3.snapshot_id = s.id AND m3.metric_id = ? " +
    "LEFT JOIN project_measures m4 " +
    "ON m4.snapshot_id = s.id AND m4.metric_id = ? " +
    "LEFT JOIN project_measures m5 " +
    "ON m5.snapshot_id = s.id AND m5.metric_id = ? " +
    "LEFT JOIN project_measures m6 " +
    "ON m6.snapshot_id = s.id AND m6.metric_id = ? " +
    "LEFT JOIN project_measures m7 " +
    "ON m7.snapshot_id = s.id AND m7.metric_id = ? " +
    "LEFT JOIN project_measures m8 " +
    "ON m8.snapshot_id = s.id AND m8.metric_id = ? " +
    "LEFT JOIN project_measures m9 " +
    "ON m9.snapshot_id = s.id AND m9.metric_id = ? " +
    "LEFT JOIN project_measures m10 " +
    "ON m10.snapshot_id = s.id AND m10.metric_id = ? " +
    "LEFT JOIN project_measures m11 " +
    "ON m11.snapshot_id = s.id AND m11.metric_id = ? " +
    "LEFT JOIN project_measures m12 " +
    "ON m12.snapshot_id = s.id AND m12.metric_id = ? " +
    "LEFT JOIN project_measures m13 " +
    "ON m13.snapshot_id = s.id AND m13.metric_id = ? " +
    "WHERE " +
    "f.enabled = ? " +
    "AND f.scope = 'FIL' " +
    "AND p.scope = 'PRJ' AND p.qualifier = 'TRK' " +
    "AND fs.file_uuid IS NULL";

  private static final class FileSourceBuilder implements MassUpdate.Handler {
    private final long now;

    public FileSourceBuilder(System2 system) {
      now = system.now();
    }

    @Override
    public boolean handle(Row row, SqlStatement update) throws SQLException {
      String projectUuid = row.getNullableString(1);
      String fileUuid = row.getNullableString(2);
      String source = StringUtils.defaultIfBlank(row.getNullableString(3), "");
      Date updatedAt = row.getNullableDate(4);
      byte[] shortRevisions = row.getNullableBytes(5);
      byte[] longRevisions = row.getNullableBytes(6);
      byte[] shortAuthors = row.getNullableBytes(7);
      byte[] longAuthors = row.getNullableBytes(8);
      byte[] shortDates = row.getNullableBytes(9);
      byte[] longDates = row.getNullableBytes(10);
      byte[] shortUtHits = row.getNullableBytes(11);
      byte[] longUtHits = row.getNullableBytes(12);
      byte[] shortUtCond = row.getNullableBytes(13);
      byte[] longUtCond = row.getNullableBytes(14);
      byte[] shortUtCovCond = row.getNullableBytes(15);
      byte[] longUtCovCond = row.getNullableBytes(16);
      byte[] shortItHits = row.getNullableBytes(17);
      byte[] longItHits = row.getNullableBytes(18);
      byte[] shortItCond = row.getNullableBytes(19);
      byte[] longItCond = row.getNullableBytes(20);
      byte[] shortItCovCond = row.getNullableBytes(21);
      byte[] longItCovCond = row.getNullableBytes(22);
      byte[] shortOverallHits = row.getNullableBytes(23);
      byte[] longOverallHits = row.getNullableBytes(24);
      byte[] shortOverallCond = row.getNullableBytes(25);
      byte[] longOverallCond = row.getNullableBytes(26);
      byte[] shortOverallCovCond = row.getNullableBytes(27);
      byte[] longOverallCovCond = row.getNullableBytes(28);
      byte[] shortDuplicationData = row.getNullableBytes(29);
      byte[] longDuplicationData = row.getNullableBytes(30);

      String[] sourceData = new FileSourceDto(source,
        ofNullableBytes(shortRevisions, longRevisions),
        ofNullableBytes(shortAuthors, longAuthors),
        ofNullableBytes(shortDates, longDates),
        ofNullableBytes(shortUtHits, longUtHits),
        ofNullableBytes(shortUtCond, longUtCond),
        ofNullableBytes(shortUtCovCond, longUtCovCond),
        ofNullableBytes(shortItHits, longItHits),
        ofNullableBytes(shortItCond, longItCond),
        ofNullableBytes(shortItCovCond, longItCovCond),
        ofNullableBytes(shortOverallHits, longOverallHits),
        ofNullableBytes(shortOverallCond, longOverallCond),
        ofNullableBytes(shortOverallCovCond, longOverallCovCond),
        ofNullableBytes(shortDuplicationData, longDuplicationData)
        ).getSourceData();

      update.setString(1, projectUuid)
        .setString(2, fileUuid)
        .setLong(3, now)
        .setLong(4, updatedAt == null ? now : updatedAt.getTime())
        .setString(5, sourceData[0])
        .setString(6, sourceData[1])
        .setString(7, "");

      return true;
    }
  }

  private static String ofNullableBytes(@Nullable byte[] shortBytes, @Nullable byte[] longBytes) {
    byte[] result;
    if (shortBytes == null) {
      if (longBytes == null) {
        return "";
      } else {
        result = longBytes;
      }
    } else {
      result = shortBytes;
    }
    return new String(result, UTF_8);
  }

  private final System2 system;

  public FeedFileSources(Database db, System2 system) {
    super(db);
    this.system = system;
  }

  @Override
  public void execute(Context context) throws SQLException {
    RowReader<Long> simpleLongReader = new RowReader<Long>() {
      @Override
      public Long read(Row row) throws SQLException {
        Long longValue = row.getNullableLong(1);
        return longValue == null ? Long.valueOf(0L) : longValue;
      }
    };
    Long revisionMetricId = context.prepareSelect("SELECT id FROM metrics WHERE name = 'revisions_by_line'").get(simpleLongReader);
    Long authorMetricId = context.prepareSelect("SELECT id FROM metrics WHERE name = 'authors_by_line'").get(simpleLongReader);
    Long datesMetricId = context.prepareSelect("SELECT id FROM metrics WHERE name = 'last_commit_datetimes_by_line'").get(simpleLongReader);
    Long utCoverageHitsByLineMetricId = context.prepareSelect("SELECT id FROM metrics WHERE name = 'coverage_line_hits_data'").get(simpleLongReader);
    Long utConditionsByLineMetricId = context.prepareSelect("SELECT id FROM metrics WHERE name = 'conditions_by_line'").get(simpleLongReader);
    Long utCoveredConditionsByLineMetricId = context.prepareSelect("SELECT id FROM metrics WHERE name = 'covered_conditions_by_line'").get(simpleLongReader);
    Long itCoverageHitsByLineMetricId = context.prepareSelect("SELECT id FROM metrics WHERE name = 'it_coverage_line_hits_data'").get(simpleLongReader);
    Long itConditionsByLineMetricId = context.prepareSelect("SELECT id FROM metrics WHERE name = 'it_conditions_by_line'").get(simpleLongReader);
    Long itCoveredConditionsByLineMetricId = context.prepareSelect("SELECT id FROM metrics WHERE name = 'it_covered_conditions_by_line'").get(simpleLongReader);
    Long overallCoverageHitsByLineMetricId = context.prepareSelect("SELECT id FROM metrics WHERE name = 'overall_coverage_line_hits_data'").get(simpleLongReader);
    Long overallConditionsByLineMetricId = context.prepareSelect("SELECT id FROM metrics WHERE name = 'overall_conditions_by_line'").get(simpleLongReader);
    Long overallCoveredConditionsByLineMetricId = context.prepareSelect("SELECT id FROM metrics WHERE name = 'overall_covered_conditions_by_line'").get(simpleLongReader);
    Long duplicationDataMetricId = context.prepareSelect("SELECT id FROM metrics WHERE name = 'duplications_data'").get(simpleLongReader);

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select(SELECT_FILES_AND_MEASURES_SQL)
      .setBoolean(1, true)
      .setLong(2, zeroIfNull(revisionMetricId))
      .setLong(3, zeroIfNull(authorMetricId))
      .setLong(4, zeroIfNull(datesMetricId))
      .setLong(5, zeroIfNull(utCoverageHitsByLineMetricId))
      .setLong(6, zeroIfNull(utConditionsByLineMetricId))
      .setLong(7, zeroIfNull(utCoveredConditionsByLineMetricId))
      .setLong(8, zeroIfNull(itCoverageHitsByLineMetricId))
      .setLong(9, zeroIfNull(itConditionsByLineMetricId))
      .setLong(10, zeroIfNull(itCoveredConditionsByLineMetricId))
      .setLong(11, zeroIfNull(overallCoverageHitsByLineMetricId))
      .setLong(12, zeroIfNull(overallConditionsByLineMetricId))
      .setLong(13, zeroIfNull(overallCoveredConditionsByLineMetricId))
      .setLong(14, zeroIfNull(duplicationDataMetricId))
      .setBoolean(15, true);

    massUpdate.update("INSERT INTO file_sources" +
      "(project_uuid, file_uuid, created_at, updated_at, data, line_hashes, data_hash)" +
      "VALUES " +
      "(?, ?, ?, ?, ?, ?, ?)");
    massUpdate.rowPluralName("files");

    massUpdate.execute(new FileSourceBuilder(system));
  }

  private static long zeroIfNull(@Nullable Long value) {
    return value == null ? 0L : value.longValue();
  }
}
