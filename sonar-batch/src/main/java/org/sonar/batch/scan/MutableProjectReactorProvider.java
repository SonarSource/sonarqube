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
package org.sonar.batch.scan;

import org.apache.commons.io.FileUtils;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.batch.bootstrap.ProjectReactor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class MutableProjectReactorProvider extends ProviderAdapter {
  private ProjectReactor reactor = null;

  public ProjectReactor provide(ProjectReactorBuilder builder) {
    if (reactor == null) {
      reactor = builder.execute();
      cleanDirectory(reactor.getRoot().getWorkDir());
    }
    return reactor;
  }

  private static void cleanDirectory(File dir) {
    try {
      FileUtils.deleteDirectory(dir);
      Files.createDirectories(dir.toPath());
    } catch (IOException e) {
      throw new IllegalStateException("Failed to recreate working directory: " + dir.getAbsolutePath(), e);
    }
  }
}
