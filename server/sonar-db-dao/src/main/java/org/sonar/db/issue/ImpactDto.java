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
package org.sonar.db.issue;

import java.io.Serializable;
import java.util.Objects;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;

public class ImpactDto implements Serializable {
  private SoftwareQuality softwareQuality;
  private Severity severity;
  private boolean manualSeverity;

  public ImpactDto() {
    // nothing to do
  }

  public ImpactDto(SoftwareQuality softwareQuality, Severity severity, boolean manualSeverity) {
    this.softwareQuality = softwareQuality;
    this.severity = severity;
    this.manualSeverity = manualSeverity;
  }

  public ImpactDto(SoftwareQuality softwareQuality, Severity severity) {
    this(softwareQuality, severity, false);
  }

  public SoftwareQuality getSoftwareQuality() {
    return softwareQuality;
  }

  public ImpactDto setSoftwareQuality(SoftwareQuality softwareQuality) {
    this.softwareQuality = softwareQuality;
    return this;
  }

  public Severity getSeverity() {
    return severity;
  }

  public ImpactDto setSeverity(Severity severity) {
    this.severity = severity;
    return this;
  }

  public boolean isManualSeverity() {
    return manualSeverity;
  }

  public ImpactDto setManualSeverity(boolean manualSeverity) {
    this.manualSeverity = manualSeverity;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ImpactDto impactDto = (ImpactDto) o;
    return Objects.equals(softwareQuality, impactDto.softwareQuality)
      && Objects.equals(severity, impactDto.severity)
      && Objects.equals(manualSeverity, impactDto.manualSeverity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(softwareQuality, severity, manualSeverity);
  }

}
