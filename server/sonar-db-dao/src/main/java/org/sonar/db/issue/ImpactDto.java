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
package org.sonar.db.issue;

import java.io.Serializable;
import java.util.Objects;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;

public class ImpactDto implements Serializable {
  private String uuid;
  private SoftwareQuality softwareQuality;
  private Severity severity;

  public ImpactDto() {
    // nothing to do
  }

  public ImpactDto(String uuid, SoftwareQuality softwareQuality, Severity severity) {
    this.uuid = uuid;
    this.softwareQuality = softwareQuality;
    this.severity = severity;
  }

  public String getUuid() {
    return uuid;
  }

  public ImpactDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ImpactDto impactDto = (ImpactDto) o;
    return Objects.equals(uuid, impactDto.uuid)
      && Objects.equals(softwareQuality, impactDto.softwareQuality)
      && Objects.equals(severity, impactDto.severity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuid, softwareQuality, severity);
  }

}
