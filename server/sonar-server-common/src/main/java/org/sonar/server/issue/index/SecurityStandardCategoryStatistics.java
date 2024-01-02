/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.Optional;
import java.util.OptionalInt;
import javax.annotation.Nullable;

public class SecurityStandardCategoryStatistics {

  private final String category;
  private final long vulnerabilities;
  private final OptionalInt vulnerabilityRating;
  private final long toReviewSecurityHotspots;
  private final long reviewedSecurityHotspots;
  private final Integer securityReviewRating;
  private final List<SecurityStandardCategoryStatistics> children;
  private long activeRules;
  private long totalRules;
  private boolean hasMoreRules;
  private final Optional<String> version;
  private Optional<String> level = Optional.empty();

  public SecurityStandardCategoryStatistics(String category, long vulnerabilities, OptionalInt vulnerabiliyRating, long toReviewSecurityHotspots,
    long reviewedSecurityHotspots, Integer securityReviewRating, @Nullable List<SecurityStandardCategoryStatistics> children, @Nullable String version) {
    this.category = category;
    this.vulnerabilities = vulnerabilities;
    this.vulnerabilityRating = vulnerabiliyRating;
    this.toReviewSecurityHotspots = toReviewSecurityHotspots;
    this.reviewedSecurityHotspots = reviewedSecurityHotspots;
    this.securityReviewRating = securityReviewRating;
    this.children = children;
    this.version = Optional.ofNullable(version);
    this.hasMoreRules = false;
  }

  public String getCategory() {
    return category;
  }

  public long getVulnerabilities() {
    return vulnerabilities;
  }

  public OptionalInt getVulnerabilityRating() {
    return vulnerabilityRating;
  }

  public long getToReviewSecurityHotspots() {
    return toReviewSecurityHotspots;
  }

  public long getReviewedSecurityHotspots() {
    return reviewedSecurityHotspots;
  }

  public Integer getSecurityReviewRating() {
    return securityReviewRating;
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

  public Optional<String> getVersion() {
    return version;
  }

  public Optional<String> getLevel() {
    return level;
  }

  public SecurityStandardCategoryStatistics setLevel(String level) {
    this.level = Optional.of(level);
    return this;
  }

  public SecurityStandardCategoryStatistics setTotalRules(long totalRules) {
    this.totalRules = totalRules;
    return this;
  }

  public boolean hasMoreRules() {
    return hasMoreRules;
  }

  public void setHasMoreRules(boolean hasMoreRules) {
    this.hasMoreRules = hasMoreRules;
  }
}
