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

import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.*;
import org.sonar.server.db.migrations.Select.Row;
import org.sonar.server.db.migrations.Select.RowReader;

import javax.annotation.Nullable;

import java.sql.SQLException;
import java.util.Date;

import static com.google.common.base.Charsets.UTF_8;

/**
 * Used in the Active Record Migration 714
 *
 * @since 5.0
 */
public class FeedFileSources extends BaseDataChange {

  private final class FileSourceBuilder implements MassUpdate.Handler {
    private final Date now;

    public FileSourceBuilder(System2 system) {
      now = new Date(system.now());
    }

    @Override
    public boolean handle(Row row, SqlStatement update) throws SQLException {
      String projectUuid = row.getString(1);
      String fileUuid = row.getString(2);
      String source = StringUtils.defaultIfBlank(row.getString(3), "");
      Date updatedAt = row.getDate(4);
      byte[] shortRevisions = row.getBytes(5);
      byte[] longRevisions = row.getBytes(6);
      byte[] shortAuthors = row.getBytes(7);
      byte[] longAuthors = row.getBytes(8);
      byte[] shortDates = row.getBytes(9);
      byte[] longDates = row.getBytes(10);
      byte[] shortUtHits = row.getBytes(11);
      byte[] longUtHits = row.getBytes(12);
      byte[] shortUtCond = row.getBytes(13);
      byte[] longUtCond = row.getBytes(14);
      byte[] shortUtCovCond = row.getBytes(15);
      byte[] longUtCovCond = row.getBytes(16);
      byte[] shortItHits = row.getBytes(17);
      byte[] longItHits = row.getBytes(18);
      byte[] shortItCond = row.getBytes(19);
      byte[] longItCond = row.getBytes(20);
      byte[] shortItCovCond = row.getBytes(21);
      byte[] longItCovCond = row.getBytes(22);
      byte[] shortOverallHits = row.getBytes(23);
      byte[] longOverallHits = row.getBytes(24);
      byte[] shortOverallCond = row.getBytes(25);
      byte[] longOverallCond = row.getBytes(26);
      byte[] shortOverallCovCond = row.getBytes(27);
      byte[] longOverallCovCond = row.getBytes(28);

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
        ofNullableBytes(shortOverallCovCond, longOverallCovCond)
        ).getSourceData();

      update.setString(1, projectUuid)
        .setString(2, fileUuid)
        .setLong(3, now.getTime())
        .setLong(4, (updatedAt == null ? now : updatedAt).getTime())
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
        Long longValue = row.getLong(1);
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

    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT " +
      "p.uuid as project_uuid, " +
      "f.uuid as file_uuid, " +
      "ss.data as source, " +
      "ss.updated_at, " +
      "m1.text_value as short_revisions_by_line, " +
      "m1.measure_data as long_revisions_by_line, " +
      "m2.text_value as short_authors_by_line, " +
      "m2.measure_data as long_authors_by_line, " +
      "m3.text_value as short_dates_by_line, " +
      "m3.measure_data as long_dates_by_line, " +
      "m4.text_value as short_hits_by_line, " +
      "m4.measure_data as long_hits_by_line, " +
      "m5.text_value as short_cond_by_line, " +
      "m5.measure_data as long_cond_by_line, " +
      "m6.text_value as short_cover_cond_by_line, " +
      "m6.measure_data as long_cover_cond_by_line, " +
      "m7.text_value as short_it_hits_by_line, " +
      "m7.measure_data as long_it_hits_by_line, " +
      "m8.text_value as short_it_cond_by_line, " +
      "m8.measure_data as long_it_cond_by_line, " +
      "m9.text_value as short_it_cover_cond_by_line, " +
      "m9.measure_data as long_it_cover_cond_by_line, " +
      "m10.text_value as short_overall_hits_by_line, " +
      "m10.measure_data as long_overall_hits_by_line, " +
      "m11.text_value as short_overall_cond_by_line, " +
      "m11.measure_data as long_overall_cond_by_line, " +
      "m12.text_value as short_overall_cover_cond_by_line, " +
      "m12.measure_data as long_overall_cover_cond_by_line " +
      "FROM snapshots s " +
      "JOIN snapshot_sources ss " +
      "ON s.id = ss.snapshot_id AND s.islast = ? " +
      "JOIN projects p " +
      "ON s.root_project_id = p.id " +
      "JOIN projects f " +
      "ON s.project_id = f.id " +
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
      "WHERE " +
      "f.enabled = ? " +
      "AND f.scope = 'FIL' " +
      "AND p.scope = 'PRJ' AND p.qualifier = 'TRK' ")
      .setBoolean(1, true)
      .setLong(2, revisionMetricId != null ? revisionMetricId : 0L)
      .setLong(3, authorMetricId != null ? authorMetricId : 0L)
      .setLong(4, datesMetricId != null ? datesMetricId : 0L)
      .setLong(5, utCoverageHitsByLineMetricId != null ? utCoverageHitsByLineMetricId : 0L)
      .setLong(6, utConditionsByLineMetricId != null ? utConditionsByLineMetricId : 0L)
      .setLong(7, utCoveredConditionsByLineMetricId != null ? utCoveredConditionsByLineMetricId : 0L)
      .setLong(8, itCoverageHitsByLineMetricId != null ? itCoverageHitsByLineMetricId : 0L)
      .setLong(9, itConditionsByLineMetricId != null ? itConditionsByLineMetricId : 0L)
      .setLong(10, itCoveredConditionsByLineMetricId != null ? itCoveredConditionsByLineMetricId : 0L)
      .setLong(11, overallCoverageHitsByLineMetricId != null ? overallCoverageHitsByLineMetricId : 0L)
      .setLong(12, overallConditionsByLineMetricId != null ? overallConditionsByLineMetricId : 0L)
      .setLong(13, overallCoveredConditionsByLineMetricId != null ? overallCoveredConditionsByLineMetricId : 0L)
      .setBoolean(14, true);

    massUpdate.update("INSERT INTO file_sources" +
      "(project_uuid, file_uuid, created_at, updated_at, data, line_hashes, data_hash)" +
      "VALUES " +
      "(?, ?, ?, ?, ?, ?, ?)");
    massUpdate.rowPluralName("files");

    massUpdate.execute(new FileSourceBuilder(system));
  }

}
