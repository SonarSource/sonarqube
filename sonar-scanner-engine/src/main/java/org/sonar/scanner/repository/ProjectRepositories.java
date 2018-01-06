/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.repository;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import java.util.Date;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public class ProjectRepositories {
  private final ImmutableTable<String, String, String> settingsByModule;
  private final ImmutableTable<String, String, FileData> fileDataByModuleAndPath;
  private final Date lastAnalysisDate;
  private final boolean exists;

  public ProjectRepositories() {
    this.exists = false;
    this.settingsByModule = new ImmutableTable.Builder<String, String, String>().build();
    this.fileDataByModuleAndPath = new ImmutableTable.Builder<String, String, FileData>().build();
    this.lastAnalysisDate = null;
  }

  public ProjectRepositories(Table<String, String, String> settingsByModule, Table<String, String, FileData> fileDataByModuleAndPath,
    @Nullable Date lastAnalysisDate) {
    this.settingsByModule = ImmutableTable.copyOf(settingsByModule);
    this.fileDataByModuleAndPath = ImmutableTable.copyOf(fileDataByModuleAndPath);
    this.lastAnalysisDate = lastAnalysisDate;
    this.exists = true;
  }

  public boolean exists() {
    return exists;
  }

  public Map<String, FileData> fileDataByPath(String moduleKey) {
    return fileDataByModuleAndPath.row(moduleKey);
  }

  public Table<String, String, FileData> fileDataByModuleAndPath() {
    return fileDataByModuleAndPath;
  }

  public boolean moduleExists(String moduleKey) {
    return settingsByModule.containsRow(moduleKey);
  }

  public Map<String, String> settings(String moduleKey) {
    return settingsByModule.row(moduleKey);
  }

  @CheckForNull
  public FileData fileData(String projectKeyWithBranch, String path) {
    return fileDataByModuleAndPath.get(projectKeyWithBranch, path);
  }

  @CheckForNull
  public Date lastAnalysisDate() {
    return lastAnalysisDate;
  }
}
