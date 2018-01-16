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
package org.sonar.scanner.scan.filesystem;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import static org.sonar.core.config.MultivalueProperty.parseAsCsv;

@ScannerSide
@Immutable
public class ModuleFileSystemInitializer {

  private static final Logger LOG = Loggers.get(ModuleFileSystemInitializer.class);

  private final List<Path> sourceDirsOrFiles;
  private final List<Path> testDirsOrFiles;
  private final Charset encoding;

  public ModuleFileSystemInitializer(DefaultInputModule inputModule) {
    logDir("Base dir: ", inputModule.getBaseDir());
    logDir("Working dir: ", inputModule.getWorkDir());
    sourceDirsOrFiles = initSources(inputModule, ProjectDefinition.SOURCES_PROPERTY, "Source paths: ");
    testDirsOrFiles = initSources(inputModule, ProjectDefinition.TESTS_PROPERTY, "Test paths: ");
    encoding = initEncoding(inputModule);
  }

  private static List<Path> initSources(DefaultInputModule module, String propertyKey, String logLabel) {
    List<Path> result = new ArrayList<>();
    PathResolver pathResolver = new PathResolver();
    String srcPropValue = module.properties().get(propertyKey);
    if (srcPropValue != null) {
      for (String sourcePath : parseAsCsv(propertyKey, srcPropValue)) {
        File dirOrFile = pathResolver.relativeFile(module.getBaseDir().toFile(), sourcePath);
        if (dirOrFile.exists()) {
          result.add(dirOrFile.toPath());
        }
      }
    }
    logPaths(logLabel, module.getBaseDir(), result);
    return result;
  }

  private static Charset initEncoding(DefaultInputModule module) {
    String encodingStr = module.properties().get(CoreProperties.ENCODING_PROPERTY);
    Charset result;
    if (StringUtils.isNotEmpty(encodingStr)) {
      result = Charset.forName(StringUtils.trim(encodingStr));
      LOG.info("Source encoding: {}, default locale: {}", result.displayName(), Locale.getDefault());
    } else {
      result = Charset.defaultCharset();
      LOG.warn("Source encoding is platform dependent ({}), default locale: {}", result.displayName(), Locale.getDefault());
    }
    return result;
  }

  List<Path> sources() {
    return sourceDirsOrFiles;
  }

  List<Path> tests() {
    return testDirsOrFiles;
  }

  public Charset defaultEncoding() {
    return encoding;
  }

  private static void logPaths(String label, Path baseDir, List<Path> paths) {
    if (!paths.isEmpty()) {
      StringBuilder sb = new StringBuilder(label);
      for (Iterator<Path> it = paths.iterator(); it.hasNext();) {
        Path file = it.next();
        Optional<String> relativePathToBaseDir = PathResolver.relativize(baseDir, file);
        if (!relativePathToBaseDir.isPresent()) {
          sb.append(file);
        } else if (StringUtils.isBlank(relativePathToBaseDir.get())) {
          sb.append(".");
        } else {
          sb.append(relativePathToBaseDir.get());
        }
        if (it.hasNext()) {
          sb.append(", ");
        }
      }
      LOG.info(sb.toString());
    }
  }

  private static void logDir(String label, @Nullable Path dir) {
    if (dir != null) {
      LOG.info(label + dir.toAbsolutePath().toString());
    }
  }

}
