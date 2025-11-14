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
package org.sonar.db.component;

public class BranchMeasuresDto {
  private String branchUuid;
  private String projectUuid;
  private String branchKey;
  private boolean excludeFromPurge;
  private int greenQualityGateCount;
  private int analysisCount;

  public BranchMeasuresDto(String branchUuid, String projectUuid, String branchKey, boolean excludeFromPurge, int greenQualityGateCount, int analysisCount) {
    this.branchUuid = branchUuid;
    this.projectUuid = projectUuid;
    this.branchKey = branchKey;
    this.excludeFromPurge = excludeFromPurge;
    this.greenQualityGateCount = greenQualityGateCount;
    this.analysisCount = analysisCount;
  }

  public String getBranchUuid() {
    return branchUuid;
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  public boolean getExcludeFromPurge() {
    return excludeFromPurge;
  }

  public int getGreenQualityGateCount() {
    return greenQualityGateCount;
  }

  public int getAnalysisCount() {
    return analysisCount;
  }

  public String getBranchKey() {
    return branchKey;
  }

}
