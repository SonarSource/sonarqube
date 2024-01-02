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
package org.sonar.ce.task.projectanalysis.pushevent;

public abstract class IssueEvent {

  private String key;
  private String projectKey;

  protected IssueEvent() {
    // nothing to do
  }

  protected IssueEvent(String key, String projectKey) {
    this.key = key;
    this.projectKey = projectKey;
  }

  public abstract String getEventName();

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getProjectKey() {
    return projectKey;
  }

  public void setProjectKey(String projectKey) {
    this.projectKey = projectKey;
  }

  @Override
  public String toString() {
    return "IssueEvent{" +
      "name='" + getEventName() + '\'' +
      ", key='" + key + '\'' +
      ", projectKey='" + projectKey + '\'' +
      '}';
  }
}
