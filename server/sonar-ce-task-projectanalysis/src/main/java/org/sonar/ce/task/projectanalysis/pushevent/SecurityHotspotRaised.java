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
package org.sonar.ce.task.projectanalysis.pushevent;

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.ce.task.projectanalysis.locations.flow.Location;

public class SecurityHotspotRaised extends HotspotEvent {

  @VisibleForTesting
  static final String EVENT_NAME = "SecurityHotspotRaised";

  private String vulnerabilityProbability;
  private long creationDate;
  private Location mainLocation;
  private String ruleKey;
  private String branch;
  private String assignee;

  public SecurityHotspotRaised() {
    // nothing to do
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  public String getVulnerabilityProbability() {
    return vulnerabilityProbability;
  }

  public void setVulnerabilityProbability(String vulnerabilityProbability) {
    this.vulnerabilityProbability = vulnerabilityProbability;
  }

  public long getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(long creationDate) {
    this.creationDate = creationDate;
  }

  public Location getMainLocation() {
    return mainLocation;
  }

  public void setMainLocation(Location mainLocation) {
    this.mainLocation = mainLocation;
  }

  public String getRuleKey() {
    return ruleKey;
  }

  public void setRuleKey(String ruleKey) {
    this.ruleKey = ruleKey;
  }

  public String getBranch() {
    return branch;
  }

  public void setBranch(String branch) {
    this.branch = branch;
  }

  public String getAssignee() {
    return assignee;
  }

  public void setAssignee(@Nullable String assignee) {
    this.assignee = assignee;
  }
}
