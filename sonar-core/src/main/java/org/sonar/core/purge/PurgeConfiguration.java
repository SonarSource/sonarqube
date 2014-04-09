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
import org.sonar.api.utils.System2;

import javax.annotation.CheckForNull;

import java.util.Date;

public class PurgeConfiguration {

  private final long rootProjectId;
  private final String[] scopesWithoutHistoricalData;
  private final int maxAgeInDaysOfClosedIssues;
  private final System2 system2;

  public PurgeConfiguration(long rootProjectId, String[] scopesWithoutHistoricalData, int maxAgeInDaysOfClosedIssues) {
    this(rootProjectId, scopesWithoutHistoricalData, maxAgeInDaysOfClosedIssues, System2.INSTANCE);
  }

  @VisibleForTesting
  PurgeConfiguration(long rootProjectId, String[] scopesWithoutHistoricalData, int maxAgeInDaysOfClosedIssues, System2 system2) {
    this.rootProjectId = rootProjectId;
    this.scopesWithoutHistoricalData = scopesWithoutHistoricalData;
    this.maxAgeInDaysOfClosedIssues = maxAgeInDaysOfClosedIssues;
    this.system2 = system2;
  }

  public long rootProjectId() {
    return rootProjectId;
  }

  public String[] scopesWithoutHistoricalData() {
    return scopesWithoutHistoricalData;
  }

  @CheckForNull
  public Date maxLiveDateOfClosedIssues() {
    return maxLiveDateOfClosedIssues(new Date(system2.now()));
  }

  @VisibleForTesting
  Date maxLiveDateOfClosedIssues(Date now) {
    if (maxAgeInDaysOfClosedIssues > 0) {
      return DateUtils.addDays(now, -maxAgeInDaysOfClosedIssues);
    }

    // delete all closed issues
    return null;
  }
}
