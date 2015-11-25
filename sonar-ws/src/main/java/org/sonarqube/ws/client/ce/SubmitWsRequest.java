/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonarqube.ws.client.ce;

import java.io.File;
import javax.annotation.CheckForNull;

public class SubmitWsRequest {

  private String projectKey;
  private String projectName;
  private String projectBranch;
  private File report;

  public String getProjectKey() {
    return projectKey;
  }

  public SubmitWsRequest setProjectKey(String projectKey) {
    this.projectKey = projectKey;
    return this;
  }

  @CheckForNull
  public String getProjectName() {
    return projectName;
  }

  public SubmitWsRequest setProjectName(String projectName) {
    this.projectName = projectName;
    return this;
  }

  @CheckForNull
  public String getProjectBranch() {
    return projectBranch;
  }

  public SubmitWsRequest setProjectBranch(String projectBranch) {
    this.projectBranch = projectBranch;
    return this;
  }

  @CheckForNull
  public File getReport() {
    return report;
  }

  public SubmitWsRequest setReport(File report) {
    this.report = report;
    return this;
  }
}
