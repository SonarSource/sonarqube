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
package org.sonar.core.purge;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.time.DateUtils;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.core.computation.dbcleaner.DbCleanerConstants;

import javax.annotation.CheckForNull;
import java.util.Date;

public class PurgeConfiguration {

  private final IdUuidPair rootProjectIdUuid;
  private final String[] scopesWithoutHistoricalData;
  private final int maxAgeInDaysOfClosedIssues;
  private final System2 system2;

  public PurgeConfiguration(IdUuidPair rootProjectId, String[] scopesWithoutHistoricalData, int maxAgeInDaysOfClosedIssues) {
    this(rootProjectId, scopesWithoutHistoricalData, maxAgeInDaysOfClosedIssues, System2.INSTANCE);
  }

  @VisibleForTesting
  PurgeConfiguration(IdUuidPair rootProjectId, String[] scopesWithoutHistoricalData, int maxAgeInDaysOfClosedIssues, System2 system2) {
    this.rootProjectIdUuid = rootProjectId;
    this.scopesWithoutHistoricalData = scopesWithoutHistoricalData;
    this.maxAgeInDaysOfClosedIssues = maxAgeInDaysOfClosedIssues;
    this.system2 = system2;
  }

  public static PurgeConfiguration newDefaultPurgeConfiguration(Settings settings, IdUuidPair idUuidPair) {
    String[] scopes = new String[]{Scopes.FILE};
    if (settings.getBoolean(DbCleanerConstants.PROPERTY_CLEAN_DIRECTORY)) {
      scopes = new String[]{Scopes.DIRECTORY, Scopes.FILE};
    }
    return new PurgeConfiguration(idUuidPair, scopes, settings.getInt(DbCleanerConstants.DAYS_BEFORE_DELETING_CLOSED_ISSUES));
  }

  public IdUuidPair rootProjectIdUuid() {
    return rootProjectIdUuid;
  }

  public String[] scopesWithoutHistoricalData() {
    return scopesWithoutHistoricalData;
  }

  @CheckForNull
  public Date maxLiveDateOfClosedIssues() {
    return maxLiveDateOfClosedIssues(new Date(system2.now()));
  }

  @VisibleForTesting
  @CheckForNull
  Date maxLiveDateOfClosedIssues(Date now) {
    if (maxAgeInDaysOfClosedIssues > 0) {
      return DateUtils.addDays(now, -maxAgeInDaysOfClosedIssues);
    }

    // delete all closed issues
    return null;
  }
}
