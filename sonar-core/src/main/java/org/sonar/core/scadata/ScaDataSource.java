/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.core.scadata;

import java.util.Collection;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Interface to access data from the SCA extension, when it is present.
 */
public interface ScaDataSource {
  /**
   * One "issue release" (dependency risk).
   */
  record IssueRelease(
    UUID uuid,
    String title,
    String projectKey,
    String projectUuid,
    String packageUrl,
    long createdAt) {
  }

  record ComponentIssueAggregations(int issueCount,
    OptionalInt issueRatingOpt) {
    public static ComponentIssueAggregations empty() {
      return new ComponentIssueAggregations(0, OptionalInt.empty());
    }

    public int totalCount() {
      return issueCount;
    }

    public Integer finalRating() {
      return issueRatingOpt.isPresent() ? issueRatingOpt.getAsInt() : null;
    }
  }

  /**
   * The component UUID could be a regular project, or it could be an application.
   * It is not yet resolved to a list of real branches.
   *
   * @param componentUuid the component UUID
   */
  ComponentIssueAggregations getComponentIssueAggregations(String componentUuid);

  /**
   * Look up details about issue-releases (aka dependency risks) by their uuids.
   * Any that aren't found simply aren't in the returned collection.
   */
  List<IssueRelease> getIssueReleasesByUuids(Collection<UUID> uuids);
}
