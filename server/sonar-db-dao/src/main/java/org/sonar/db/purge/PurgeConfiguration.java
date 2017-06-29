/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.db.purge;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collection;
import java.util.Date;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.time.DateUtils;
import org.sonar.api.config.Configuration;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.core.config.PurgeConstants;

public class PurgeConfiguration {

  private final IdUuidPair rootProjectIdUuid;
  private final String[] scopesWithoutHistoricalData;
  private final int maxAgeInDaysOfClosedIssues;
  private final System2 system2;
  private final Collection<String> disabledComponentUuids;

  public PurgeConfiguration(IdUuidPair rootProjectId, String[] scopesWithoutHistoricalData, int maxAgeInDaysOfClosedIssues,
    System2 system2, Collection<String> disabledComponentUuids) {
    this.rootProjectIdUuid = rootProjectId;
    this.scopesWithoutHistoricalData = scopesWithoutHistoricalData;
    this.maxAgeInDaysOfClosedIssues = maxAgeInDaysOfClosedIssues;
    this.system2 = system2;
    this.disabledComponentUuids = disabledComponentUuids;
  }

  public static PurgeConfiguration newDefaultPurgeConfiguration(Configuration config, IdUuidPair idUuidPair, Collection<String> disabledComponentUuids) {
    String[] scopes = new String[] {Scopes.FILE};
    if (config.getBoolean(PurgeConstants.PROPERTY_CLEAN_DIRECTORY).orElse(false)) {
      scopes = new String[] {Scopes.DIRECTORY, Scopes.FILE};
    }
    return new PurgeConfiguration(idUuidPair, scopes, config.getInt(PurgeConstants.DAYS_BEFORE_DELETING_CLOSED_ISSUES).get(), System2.INSTANCE, disabledComponentUuids);
  }

  public IdUuidPair rootProjectIdUuid() {
    return rootProjectIdUuid;
  }

  public String[] scopesWithoutHistoricalData() {
    return scopesWithoutHistoricalData;
  }

  public Collection<String> getDisabledComponentUuids() {
    return disabledComponentUuids;
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
