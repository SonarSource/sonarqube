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
package org.sonar.db.purge;

import com.google.common.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.api.config.Configuration;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.config.PurgeConstants;

public class PurgeConfiguration {

  private final String rootUuid;
  private final String projectUuid;
  private final Collection<String> scopesWithoutHistoricalData;
  private final int maxAgeInDaysOfClosedIssues;
  private final Optional<Integer> maxAgeInDaysOfInactiveShortLivingBranches;
  private final System2 system2;
  private final Set<String> disabledComponentUuids;

  public PurgeConfiguration(String rootUuid, String projectUuid, Collection<String> scopesWithoutHistoricalData, int maxAgeInDaysOfClosedIssues,
    Optional<Integer> maxAgeInDaysOfInactiveShortLivingBranches, System2 system2, Set<String> disabledComponentUuids) {
    this.rootUuid = rootUuid;
    this.projectUuid = projectUuid;
    this.scopesWithoutHistoricalData = scopesWithoutHistoricalData;
    this.maxAgeInDaysOfClosedIssues = maxAgeInDaysOfClosedIssues;
    this.system2 = system2;
    this.disabledComponentUuids = disabledComponentUuids;
    this.maxAgeInDaysOfInactiveShortLivingBranches = maxAgeInDaysOfInactiveShortLivingBranches;
  }

  public static PurgeConfiguration newDefaultPurgeConfiguration(Configuration config, String rootUuid, String projectUuid, Set<String> disabledComponentUuids) {
    return new PurgeConfiguration(rootUuid, projectUuid, Arrays.asList(Scopes.DIRECTORY, Scopes.FILE), config.getInt(PurgeConstants.DAYS_BEFORE_DELETING_CLOSED_ISSUES).get(),
      config.getInt(PurgeConstants.DAYS_BEFORE_DELETING_INACTIVE_SHORT_LIVING_BRANCHES), System2.INSTANCE, disabledComponentUuids);
  }

  /**
   * UUID of the branch being analyzed (root of the component tree). Will be the same as {@link #projectUuid}
   * if it's the main branch.
   * Can also be a view.
   */
  public String rootUuid() {
    return rootUuid;
  }

  /**
   * @return UUID of the main branch of the project
   */
  public String projectUuid() {
    return projectUuid;
  }

  public Collection<String> getScopesWithoutHistoricalData() {
    return scopesWithoutHistoricalData;
  }

  public Set<String> getDisabledComponentUuids() {
    return disabledComponentUuids;
  }

  @CheckForNull
  public Date maxLiveDateOfClosedIssues() {
    return maxLiveDateOfClosedIssues(new Date(system2.now()));
  }

  public Optional<Date> maxLiveDateOfInactiveShortLivingBranches() {
    return maxAgeInDaysOfInactiveShortLivingBranches.map(age -> DateUtils.addDays(new Date(system2.now()), -age));
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
