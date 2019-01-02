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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.Map;
import java.util.stream.Stream;
import org.sonar.core.issue.DefaultIssue;

public class TrackingResult {
  private final Map<DefaultIssue, DefaultIssue> issuesToCopy;
  private final Map<DefaultIssue, DefaultIssue> issuesToMerge;
  private final Stream<DefaultIssue> issuesToClose;
  private final Stream<DefaultIssue> newIssues;

  public TrackingResult(Map<DefaultIssue, DefaultIssue> issuesToCopy, Map<DefaultIssue, DefaultIssue> issuesToMerge,
    Stream<DefaultIssue> issuesToClose, Stream<DefaultIssue> newIssues) {
    this.issuesToCopy = issuesToCopy;
    this.issuesToMerge = issuesToMerge;
    this.issuesToClose = issuesToClose;
    this.newIssues = newIssues;
  }

  public Map<DefaultIssue, DefaultIssue> issuesToCopy() {
    return issuesToCopy;
  }

  public Map<DefaultIssue, DefaultIssue> issuesToMerge() {
    return issuesToMerge;
  }

  public Stream<DefaultIssue> issuesToClose() {
    return issuesToClose;
  }

  public Stream<DefaultIssue> newIssues() {
    return newIssues;
  }
}
