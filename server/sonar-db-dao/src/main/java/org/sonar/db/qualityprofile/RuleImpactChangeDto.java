/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.db.qualityprofile;

public class RuleImpactChangeDto {

  private String newSoftwareQuality;
  private String oldSoftwareQuality;

  private String newSeverity;
  private String oldSeverity;

  private String ruleChangeUuid;

  public String getNewSoftwareQuality() {
    return newSoftwareQuality;
  }

  public void setNewSoftwareQuality(String newSoftwareQuality) {
    this.newSoftwareQuality = newSoftwareQuality;
  }

  public String getOldSoftwareQuality() {
    return oldSoftwareQuality;
  }

  public void setOldSoftwareQuality(String oldSoftwareQuality) {
    this.oldSoftwareQuality = oldSoftwareQuality;
  }

  public String getNewSeverity() {
    return newSeverity;
  }

  public void setNewSeverity(String newSeverity) {
    this.newSeverity = newSeverity;
  }

  public String getOldSeverity() {
    return oldSeverity;
  }

  public void setOldSeverity(String oldSeverity) {
    this.oldSeverity = oldSeverity;
  }

  public String getRuleChangeUuid() {
    return ruleChangeUuid;
  }

  public void setRuleChangeUuid(String ruleChangeUuid) {
    this.ruleChangeUuid = ruleChangeUuid;
  }
}
