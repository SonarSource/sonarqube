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

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import javax.annotation.Nullable;

import org.sonar.batch.analysis.DefaultAnalysisMode;
import org.sonar.batch.protocol.input.FileData;
import com.google.common.collect.Table;
import com.google.common.collect.ImmutableTable;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.picocontainer.injectors.ProviderAdapter;

public class ProjectSettingsProvider extends ProviderAdapter {
  private static final Logger LOG = Loggers.get(ProjectSettingsProvider.class);
  private ProjectSettingsRepo settings = null;

  public ProjectSettingsRepo provide(@Nullable ProjectSettingsLoader loader, ProjectReactor projectReactor, DefaultAnalysisMode mode) {
    if (settings == null) {
      if (mode.isNotAssociated()) {
        settings = createNonAssociatedProjectSettings();
      } else {
        MutableBoolean fromCache = new MutableBoolean();
        settings = loader.load(projectReactor.getRoot().getKeyWithBranch(), fromCache);
        checkProject(mode);
      }
    }

    return settings;
  }

  private void checkProject(DefaultAnalysisMode mode) {
    if (mode.isIssues() && settings.lastAnalysisDate() == null) {
      LOG.warn("No analysis has been found on the server for this project. All issues will be marked as 'new'.");
    }
  }

  private static ProjectSettingsRepo createNonAssociatedProjectSettings() {
    Table<String, String, String> emptySettings = ImmutableTable.of();
    Table<String, String, FileData> emptyFileData = ImmutableTable.of();
    return new ProjectSettingsRepo(emptySettings, emptyFileData, null);
  }
}
