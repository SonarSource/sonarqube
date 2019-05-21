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
package org.sonar.server.platform.db.migration.version.v78;

import com.google.common.collect.Maps;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.es.MigrationEsClient;
import org.sonar.server.platform.db.migration.step.DataChange;
import org.sonar.server.platform.db.migration.step.MassUpdate;

@SupportsBlueGreen
public class UpdateSecurityHotspotsStatuses extends DataChange {

  private static final String RESOLUTION_FIXED = "FIXED";
  private static final String RESOLUTION_WONT_FIX = "WONTFIX";

  private static final String STATUS_OPEN = "OPEN";
  private static final String STATUS_REOPENED = "REOPENED";
  private static final String STATUS_RESOLVED = "RESOLVED";

  private static final String STATUS_TO_REVIEW = "TO_REVIEW";
  private static final String STATUS_IN_REVIEW = "IN_REVIEW";
  private static final String STATUS_REVIEWED = "REVIEWED";

  private static final int RULE_TYPE_SECURITY_HOTSPOT = 4;

  private final Configuration configuration;
  private final System2 system2;
  private final MigrationEsClient esClient;
  private final UuidFactory uuidFactory;

  public UpdateSecurityHotspotsStatuses(Database db, Configuration configuration, System2 system2, MigrationEsClient esClient, UuidFactory uuidFactory) {
    super(db);
    this.configuration = configuration;
    this.system2 = system2;
    this.esClient = esClient;
    this.uuidFactory = uuidFactory;
  }

  @Override
  public void execute(Context context) throws SQLException {
    if (configuration.getBoolean("sonar.sonarcloud.enabled").orElse(false)) {
      return;
    }
    long now = system2.now();
    MassUpdate massUpdate = context.prepareMassUpdate().rowPluralName("security hotspots");
    massUpdate.select("select i.kee, i.status, i.resolution, i.issue_type from issues i " +
      "inner join rules r on r.id = i.rule_id and r.rule_type = ? " +
      "where (i.resolution is null or i.resolution in (?, ?)) and i.issue_type=? " +
      // Add status check for the re-entrance, in order to not reload already migrated issues
      "and i.status not in (?, ?, ?)")
      .setInt(1, RULE_TYPE_SECURITY_HOTSPOT)
      .setString(2, RESOLUTION_FIXED)
      .setString(3, RESOLUTION_WONT_FIX)
      .setInt(4, RULE_TYPE_SECURITY_HOTSPOT)
      .setString(5, STATUS_TO_REVIEW)
      .setString(6, STATUS_IN_REVIEW)
      .setString(7, STATUS_REVIEWED);
    massUpdate.update("update issues set status=?, resolution=?, updated_at=? where kee=? ");
    massUpdate.update("insert into issue_changes (kee, issue_key, change_type, change_data, created_at, updated_at, issue_change_creation_date) values (?, ?, 'diff', ?, ?, ?, ?)");
    massUpdate.execute((row, update, updateIndex) -> {
      String issueKey = row.getString(1);
      String status = row.getString(2);
      String resolution = row.getNullableString(3);

      IssueUpdate issueUpdate = new IssueUpdate(status, resolution);
      FieldDiffs fieldDiffs = issueUpdate.process();
      if (!issueUpdate.isUpdated()) {
        return false;
      }
      if (updateIndex == 0) {
        update.setString(1, issueUpdate.getNewStatus());
        update.setString(2, issueUpdate.getNewResolution());
        update.setLong(3, now);
        update.setString(4, issueKey);
        return true;
      } else {
        // No changelog on OPEN issue as there was no previous state
        if (!status.equals(STATUS_OPEN)) {
          update.setString(1, uuidFactory.create());
          update.setString(2, issueKey);
          update.setString(3, fieldDiffs.toEncodedString());
          update.setLong(4, now);
          update.setLong(5, now);
          update.setLong(6, now);
          return true;
        }
        return false;
      }
    });
    esClient.deleteIndexes("issues");
  }

  private static class IssueUpdate {

    private static final String RESOLUTION_FIELD = "resolution";
    private static final String STATUS_FIELD = "status";

    private final String status;
    private final String resolution;

    private String newStatus;
    private String newResolution;
    private boolean updated;

    IssueUpdate(String status, @CheckForNull String resolution) {
      this.status = status;
      this.resolution = resolution;
    }

    FieldDiffs process() {
      if ((status.equals(STATUS_OPEN) || (status.equals(STATUS_REOPENED))) && resolution == null) {
        newStatus = STATUS_TO_REVIEW;
        newResolution = null;
        updated = true;
      } else if (status.equals(STATUS_RESOLVED) && resolution != null) {
        if (resolution.equals(RESOLUTION_FIXED)) {
          newStatus = STATUS_IN_REVIEW;
          newResolution = null;
          updated = true;
        } else if (resolution.equals(RESOLUTION_WONT_FIX)) {
          newStatus = STATUS_REVIEWED;
          newResolution = RESOLUTION_FIXED;
          updated = true;
        }
      }
      FieldDiffs fieldDiffs = new FieldDiffs();
      fieldDiffs.setDiff(STATUS_FIELD, status, newStatus);
      fieldDiffs.setDiff(RESOLUTION_FIELD, resolution, newResolution);
      return fieldDiffs;
    }

    String getNewStatus() {
      return newStatus;
    }

    String getNewResolution() {
      return newResolution;
    }

    boolean isUpdated() {
      return updated;
    }
  }

  /**
   * Inspired and simplified from {@link org.sonar.core.issue.FieldDiffs}
   */
  static class FieldDiffs implements Serializable {

    private final Map<String, Diff> diffs = Maps.newLinkedHashMap();

    void setDiff(String field, @Nullable String oldValue, @Nullable String newValue) {
      diffs.put(field, new Diff(oldValue, newValue));
    }

    String toEncodedString() {
      StringBuilder sb = new StringBuilder();
      boolean notFirst = false;
      for (Map.Entry<String, FieldDiffs.Diff> entry : diffs.entrySet()) {
        if (notFirst) {
          sb.append(',');
        } else {
          notFirst = true;
        }
        sb.append(entry.getKey());
        sb.append('=');
        sb.append(entry.getValue().toEncodedString());
      }
      return sb.toString();
    }

    static class Diff implements Serializable {
      private String oldValue;
      private String newValue;

      Diff(@Nullable String oldValue, @Nullable String newValue) {
        this.oldValue = oldValue;
        this.newValue = newValue;
      }

      private String toEncodedString() {
        StringBuilder sb = new StringBuilder();
        if (oldValue != null) {
          sb.append(oldValue);
          sb.append('|');
        }
        if (newValue != null) {
          sb.append(newValue);
        }
        return sb.toString();
      }

      @Override
      public boolean equals(Object o) {
        if (this == o) {
          return true;
        }
        if (o == null || getClass() != o.getClass()) {
          return false;
        }
        FieldDiffs.Diff diff = (FieldDiffs.Diff) o;
        return Objects.equals(oldValue, diff.oldValue) &&
          Objects.equals(newValue, diff.newValue);
      }

      @Override
      public int hashCode() {
        return Objects.hash(oldValue, newValue);
      }
    }

  }

}
