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
package org.sonar.ce.task.projectexport.util;

import java.io.File;
import org.sonar.api.Startable;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.ce.task.projectexport.taskprocessor.ProjectDescriptor;
import org.sonar.ce.task.util.Files2;

import static org.sonar.core.util.Slug.slugify;
import static org.sonar.process.ProcessProperties.Property.PATH_DATA;

@ServerSide
@ComputeEngineSide
public class ProjectExportDumpFSImpl implements ProjectExportDumpFS, Startable {
  private static final String GOVERNANCE_DIR_NAME = "governance";
  private static final String PROJECT_DUMPS_DIR_NAME = "project_dumps";
  private static final String DUMP_FILE_EXTENSION = ".zip";

  private final File exportDir;

  public ProjectExportDumpFSImpl(Configuration config) {
    String dataPath = config.get(PATH_DATA.getKey()).get();
    File governanceDir = new File(dataPath, GOVERNANCE_DIR_NAME);
    File projectDumpDir = new File(governanceDir, PROJECT_DUMPS_DIR_NAME);
    this.exportDir = new File(projectDumpDir, "export");
  }

  @Override
  public void start() {
    Files2.FILES2.createDir(exportDir);
  }

  @Override
  public void stop() {
    // nothing to do
  }

  @Override
  public File exportDumpOf(ProjectDescriptor descriptor) {
    String fileName = slugify(descriptor.getKey()) + DUMP_FILE_EXTENSION;
    return new File(exportDir, fileName);
  }
}
