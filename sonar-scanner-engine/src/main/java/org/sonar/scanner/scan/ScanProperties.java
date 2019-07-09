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
package org.sonar.scanner.scan;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.MessageException;
import org.sonar.api.batch.fs.internal.DefaultInputProject;

import static org.sonar.core.config.ScannerProperties.BRANCH_NAME;
import static org.sonar.core.config.ScannerProperties.ORGANIZATION;

/**
 * Properties that can be passed to the scanners and are not exposed in SonarQube.
 */
public class ScanProperties {
  public static final String METADATA_FILE_PATH_KEY = "sonar.scanner.metadataFilePath";
  public static final String KEEP_REPORT_PROP_KEY = "sonar.scanner.keepReport";
  public static final String VERBOSE_KEY = "sonar.verbose";
  public static final String METADATA_DUMP_FILENAME = "report-task.txt";
  public static final String SONAR_REPORT_EXPORT_PATH = "sonar.report.export.path";
  public static final String PRELOAD_FILE_METADATA_KEY = "sonar.preloadFileMetadata";
  public static final String FORCE_RELOAD_KEY = "sonar.scm.forceReloadAll";
  public static final String SCM_REVISION = "sonar.scm.revision";

  private final Configuration configuration;
  private final DefaultInputProject project;

  public ScanProperties(Configuration configuration, DefaultInputProject project) {
    this.configuration = configuration;
    this.project = project;
  }

  public boolean shouldKeepReport() {
    return configuration.getBoolean(KEEP_REPORT_PROP_KEY).orElse(false) || configuration.getBoolean(VERBOSE_KEY).orElse(false);
  }

  public boolean preloadFileMetadata() {
    return configuration.getBoolean(PRELOAD_FILE_METADATA_KEY).orElse(false);
  }

  public Optional<String> organizationKey() {
    return configuration.get(ORGANIZATION);
  }

  public Optional<String> branch() {
    return configuration.get(BRANCH_NAME);
  }

  public Optional<String> get(String propertyKey) {
    return configuration.get(propertyKey);
  }

  public Path metadataFilePath() {
    Optional<String> metadataFilePath = configuration.get(METADATA_FILE_PATH_KEY);
    if (metadataFilePath.isPresent()) {
      Path metadataPath = Paths.get(metadataFilePath.get());
      if (!metadataPath.isAbsolute()) {
        throw MessageException.of(String.format("Property '%s' must point to an absolute path: %s", METADATA_FILE_PATH_KEY, metadataFilePath.get()));
      }
      return project.getBaseDir().resolve(metadataPath);
    } else {
      return project.getWorkDir().resolve(METADATA_DUMP_FILENAME);
    }
  }

  /**
   * This should be called in the beginning of the analysis to fail fast
   */
  public void validate() {
    metadataFilePath();
  }
}
