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
package org.sonar.scanner.sensor;

import javax.annotation.concurrent.ThreadSafe;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.cache.ReadCache;
import org.sonar.api.batch.sensor.cache.WriteCache;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.Settings;
import org.sonar.scanner.cache.AnalysisCacheEnabled;
import org.sonar.scanner.scan.branch.BranchConfiguration;

@ThreadSafe
public class ModuleSensorContext extends ProjectSensorContext {

  private final InputModule module;

  public ModuleSensorContext(DefaultInputProject project, InputModule module, Configuration config, Settings mutableModuleSettings, FileSystem fs, ActiveRules activeRules,
    DefaultSensorStorage sensorStorage, SonarRuntime sonarRuntime, BranchConfiguration branchConfiguration,
    WriteCache writeCache, ReadCache readCache, AnalysisCacheEnabled analysisCacheEnabled, UnchangedFilesHandler unchangedFilesHandler) {
    super(project, config, mutableModuleSettings, fs, activeRules, sensorStorage, sonarRuntime, branchConfiguration, writeCache, readCache, analysisCacheEnabled,
      unchangedFilesHandler);
    this.module = module;
  }

  @Override
  public InputModule module() {
    return module;
  }

}
