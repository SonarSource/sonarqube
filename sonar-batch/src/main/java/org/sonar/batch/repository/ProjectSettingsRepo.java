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
package org.sonar.batch.repository;

import com.google.common.collect.Table;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.sonar.batch.protocol.input.FileData;

import java.util.Date;
import java.util.Map;

public class ProjectSettingsRepo {
  private Table<String, String, String> settingsByModule = null;
  private Table<String, String, FileData> fileDataByModuleAndPath = null;
  private Date lastAnalysisDate;

  public ProjectSettingsRepo(Table<String, String, String> settingsByModule, Table<String, String, FileData> fileDataByModuleAndPath,
    @Nullable Date lastAnalysisDate) {
    super();
    this.settingsByModule = settingsByModule;
    this.fileDataByModuleAndPath = fileDataByModuleAndPath;
    this.lastAnalysisDate = lastAnalysisDate;
  }

  public Map<String, FileData> fileDataByPath(String moduleKey) {
    return fileDataByModuleAndPath.row(moduleKey);
  }

  public Table<String, String, FileData> fileDataByModuleAndPath() {
    return fileDataByModuleAndPath;
  }

  public Map<String, String> settings(String moduleKey) {
    return settingsByModule.row(moduleKey);
  }

  @CheckForNull
  public FileData fileData(String projectKey, String path) {
    return fileDataByModuleAndPath.get(projectKey, path);
  }

  @CheckForNull
  public Date lastAnalysisDate() {
    return lastAnalysisDate;
  }
}
