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
package org.sonar.db.rule;

import java.util.Objects;
import javax.annotation.Nullable;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;

public class RuleImpactChangeDto {

  private SoftwareQuality newSoftwareQuality;
  private SoftwareQuality oldSoftwareQuality;

  private Severity newSeverity;
  private Severity oldSeverity;

  private String ruleChangeUuid;

  public RuleImpactChangeDto() {
  }

  public RuleImpactChangeDto(@Nullable SoftwareQuality newSoftwareQuality, @Nullable SoftwareQuality oldSoftwareQuality,
    @Nullable Severity newSeverity, @Nullable Severity oldSeverity) {
    this.newSoftwareQuality = newSoftwareQuality;
    this.oldSoftwareQuality = oldSoftwareQuality;
    this.newSeverity = newSeverity;
    this.oldSeverity = oldSeverity;
  }

  public SoftwareQuality getNewSoftwareQuality() {
    return newSoftwareQuality;
  }

  public void setNewSoftwareQuality(SoftwareQuality newSoftwareQuality) {
    this.newSoftwareQuality = newSoftwareQuality;
  }

  public SoftwareQuality getOldSoftwareQuality() {
    return oldSoftwareQuality;
  }

  public void setOldSoftwareQuality(SoftwareQuality oldSoftwareQuality) {
    this.oldSoftwareQuality = oldSoftwareQuality;
  }

  public Severity getNewSeverity() {
    return newSeverity;
  }

  public void setNewSeverity(Severity newSeverity) {
    this.newSeverity = newSeverity;
  }

  public Severity getOldSeverity() {
    return oldSeverity;
  }

  public void setOldSeverity(Severity oldSeverity) {
    this.oldSeverity = oldSeverity;
  }

  public String getRuleChangeUuid() {
    return ruleChangeUuid;
  }

  public void setRuleChangeUuid(String ruleChangeUuid) {
    this.ruleChangeUuid = ruleChangeUuid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RuleImpactChangeDto that = (RuleImpactChangeDto) o;
    return newSoftwareQuality == that.newSoftwareQuality && oldSoftwareQuality == that.oldSoftwareQuality && newSeverity == that.newSeverity && oldSeverity == that.oldSeverity
      && Objects.equals(ruleChangeUuid, that.ruleChangeUuid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(newSoftwareQuality, oldSoftwareQuality, newSeverity, oldSeverity, ruleChangeUuid);
  }
}
