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
package org.sonar.db.permission.template;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public class DefaultTemplates {
  // Hold the template uuid for new projects
  private String projectUuid;
  // Hold the template uuid for new applications
  private String applicationsUuid;
  // Hold the template uuid for new portfolios
  private String portfoliosUuid;

  public DefaultTemplates() {
    // nothing to do here
  }

  public String getProjectUuid() {
    checkProjectNotNull(this.projectUuid);
    return this.projectUuid;
  }

  public DefaultTemplates setProjectUuid(String projectUuid) {
    checkProjectNotNull(projectUuid);
    this.projectUuid = projectUuid;
    return this;
  }

  private static void checkProjectNotNull(String project) {
    requireNonNull(project, "defaultTemplates.project can't be null");
  }

  @CheckForNull
  public String getApplicationsUuid() {
    return applicationsUuid;
  }

  public DefaultTemplates setApplicationsUuid(@Nullable String applicationsUuid) {
    this.applicationsUuid = applicationsUuid;
    return this;
  }

  @CheckForNull
  public String getPortfoliosUuid() {
    return portfoliosUuid;
  }

  public DefaultTemplates setPortfoliosUuid(@Nullable String portfoliosUuid) {
    this.portfoliosUuid = portfoliosUuid;
    return this;
  }

  @Override
  public String toString() {
    return "DefaultTemplates{" +
      "projectUuid='" + projectUuid + '\'' +
      ", portfoliosUuid='" + portfoliosUuid + '\'' +
      ", applicationsUuid='" + applicationsUuid + '\'' +
      '}';
  }
}
