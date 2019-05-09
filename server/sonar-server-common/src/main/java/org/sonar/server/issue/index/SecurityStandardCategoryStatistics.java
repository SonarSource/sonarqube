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
package org.sonar.server.issue.index;

import java.util.List;
import java.util.OptionalInt;
import javax.annotation.Nullable;

public class SecurityStandardCategoryStatistics {

  private final String category;
  private final long vulnerabilities;
  private final OptionalInt vulnerabiliyRating;
  private final long inReviewSecurityHotspots;
  private final long toReviewSecurityHotspots;
  private final long reviewedSecurityHotspots;
  private final List<SecurityStandardCategoryStatistics> children;
  private long activeRules;
  private long totalRules;

  public SecurityStandardCategoryStatistics(String category, long vulnerabilities, OptionalInt vulnerabiliyRating, long inReviewSecurityHotspots, long toReviewSecurityHotspots,
    long reviewedSecurityHotspots, @Nullable List<SecurityStandardCategoryStatistics> children) {
    this.category = category;
    this.vulnerabilities = vulnerabilities;
    this.vulnerabiliyRating = vulnerabiliyRating;
    this.inReviewSecurityHotspots = inReviewSecurityHotspots;
    this.toReviewSecurityHotspots = toReviewSecurityHotspots;
    this.reviewedSecurityHotspots = reviewedSecurityHotspots;
    this.children = children;
  }

  public String getCategory() {
    return category;
  }

  public long getVulnerabilities() {
    return vulnerabilities;
  }

  public OptionalInt getVulnerabiliyRating() {
    return vulnerabiliyRating;
  }

  public long getInReviewSecurityHotspots() {
    return inReviewSecurityHotspots;
  }

  public long getToReviewSecurityHotspots() {
    return toReviewSecurityHotspots;
  }

  public long getReviewedSecurityHotspots() {
    return reviewedSecurityHotspots;
  }

  public List<SecurityStandardCategoryStatistics> getChildren() {
    return children;
  }

  public long getActiveRules() {
    return activeRules;
  }

  public SecurityStandardCategoryStatistics setActiveRules(long activeRules) {
    this.activeRules = activeRules;
    return this;
  }

  public long getTotalRules() {
    return totalRules;
  }

  public SecurityStandardCategoryStatistics setTotalRules(long totalRules) {
    this.totalRules = totalRules;
    return this;
  }
}
