/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.api.batch.fs.internal;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.scan.filesystem.PathResolver;

import static org.sonar.api.config.internal.MultivalueProperty.parseAsCsv;

@Immutable
public class DefaultInputModule extends AbstractProjectOrModule implements InputModule {

  private final List<Path> sourceDirsOrFiles;
  private final List<Path> testDirsOrFiles;

  /**
   * For testing only!
   */
  public DefaultInputModule(ProjectDefinition definition) {
    this(definition, 0);
  }

  public DefaultInputModule(ProjectDefinition definition, int scannerComponentId) {
    super(definition, scannerComponentId);

    this.sourceDirsOrFiles = initSources(definition, ProjectDefinition.SOURCES_PROPERTY);
    this.testDirsOrFiles = initSources(definition, ProjectDefinition.TESTS_PROPERTY);
  }

  @CheckForNull
  private List<Path> initSources(ProjectDefinition module, String propertyKey) {
    if (!module.properties().containsKey(propertyKey)) {
      return null;
    }
    List<Path> result = new ArrayList<>();
    PathResolver pathResolver = new PathResolver();
    String srcPropValue = module.properties().get(propertyKey);
    if (srcPropValue != null) {
      for (String sourcePath : parseAsCsv(propertyKey, srcPropValue)) {
        File dirOrFile = pathResolver.relativeFile(getBaseDir().toFile(), sourcePath);
        if (dirOrFile.exists()) {
          result.add(dirOrFile.toPath());
        }
      }
    }
    return result;
  }

  public Optional<List<Path>> getSourceDirsOrFiles() {
    return Optional.ofNullable(sourceDirsOrFiles);
  }

  public Optional<List<Path>> getTestDirsOrFiles() {
    return Optional.ofNullable(testDirsOrFiles);
  }
}
