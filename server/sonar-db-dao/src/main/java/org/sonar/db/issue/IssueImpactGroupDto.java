/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db.issue;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;

public class IssueImpactGroupDto {

  private String status;
  private String resolution;
  private SoftwareQuality softwareQuality;
  private Severity severity;
  private long count;
  private boolean inLeak;
  private double effort;

  public IssueImpactGroupDto() {
    // nothing to do
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @CheckForNull
  public String getResolution() {
    return resolution;
  }

  public void setResolution(@Nullable String resolution) {
    this.resolution = resolution;
  }

  public SoftwareQuality getSoftwareQuality() {
    return softwareQuality;
  }

  public void setSoftwareQuality(SoftwareQuality softwareQuality) {
    this.softwareQuality = softwareQuality;
  }

  public Severity getSeverity() {
    return severity;
  }

  public void setSeverity(Severity severity) {
    this.severity = severity;
  }

  public long getCount() {
    return count;
  }

  public void setCount(long count) {
    this.count = count;
  }

  public boolean isInLeak() {
    return inLeak;
  }

  public void setInLeak(boolean inLeak) {
    this.inLeak = inLeak;
  }

  public double getEffort() {
    return effort;
  }

  public void setEffort(double effort) {
    this.effort = effort;
  }
}
