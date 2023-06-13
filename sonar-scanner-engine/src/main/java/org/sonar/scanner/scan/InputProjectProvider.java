/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.scan;

import java.util.Locale;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.scanner.scan.filesystem.ScannerComponentIdGenerator;
import org.springframework.context.annotation.Bean;

public class InputProjectProvider {
  private static final Logger LOG = LoggerFactory.getLogger(InputProjectProvider.class);

  @Bean("DefaultInputProject")
  public DefaultInputProject provide(ProjectBuildersExecutor projectBuildersExecutor, ProjectReactorValidator validator,
    ProjectReactor projectReactor, ScannerComponentIdGenerator scannerComponentIdGenerator, WorkDirectoriesInitializer workDirectoriesInit) {
    // 1 Apply project builders
    projectBuildersExecutor.execute(projectReactor);

    // 2 Validate final reactor
    validator.validate(projectReactor);

    // 3 Create project
    DefaultInputProject project = new DefaultInputProject(projectReactor.getRoot(), scannerComponentIdGenerator.getAsInt());
    workDirectoriesInit.execute(project);

    LOG.info("Project key: {}", project.key());
    LOG.info("Base dir: {}", project.getBaseDir().toAbsolutePath().toString());
    LOG.info("Working dir: {}", project.getWorkDir().toAbsolutePath().toString());
    LOG.debug("Project global encoding: {}, default locale: {}", project.getEncoding().displayName(), Locale.getDefault());
    return project;
  }
}
