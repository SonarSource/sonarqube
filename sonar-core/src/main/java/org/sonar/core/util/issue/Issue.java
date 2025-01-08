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
package org.sonar.core.util.issue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;

public class Issue implements Serializable {
  private final String issueKey;
  private final String branchName;
  private final List<Impact> impacts = new ArrayList<>();

  public Issue(String issueKey, String branchName) {
    this.issueKey = issueKey;
    this.branchName = branchName;
  }

  public String getIssueKey() {
    return issueKey;
  }

  public String getBranchName() {
    return branchName;
  }

  public void addImpact(SoftwareQuality quality, Severity severity) {
    impacts.add(new Impact(quality, severity));
  }

  public List<Impact> getImpacts() {
    return impacts;
  }

  public record Impact(SoftwareQuality softwareQuality, Severity severity) {
  }

}
