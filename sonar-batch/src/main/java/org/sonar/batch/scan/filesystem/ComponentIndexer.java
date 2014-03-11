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
package org.sonar.batch.scan.filesystem;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.io.Files;
import org.sonar.api.BatchComponent;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.*;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.index.ResourceKeyMigration;

/**
 * Index all files/directories of the module in SQ database and importing source code.
 * @since 4.2
 */
public class ComponentIndexer implements BatchComponent {

  private final Languages languages;
  private final Settings settings;
  private final SonarIndex sonarIndex;
  private final ResourceKeyMigration migration;
  private final Project module;
  private InputFileCache fileCache;

  public ComponentIndexer(Project module, Languages languages, SonarIndex sonarIndex, Settings settings, ResourceKeyMigration migration,
                          InputFileCache fileCache) {
    this.module = module;
    this.languages = languages;
    this.sonarIndex = sonarIndex;
    this.settings = settings;
    this.migration = migration;
    this.fileCache = fileCache;
  }

  public void execute(FileSystem fs) {
    migration.migrateIfNeeded(module, fs);

    boolean shouldImportSource = settings.getBoolean(CoreProperties.CORE_IMPORT_SOURCES_PROPERTY);
    for (InputFile inputFile : fs.inputFiles(FilePredicates.all())) {
      String languageKey = inputFile.language();
      boolean unitTest = InputFile.Type.TEST == inputFile.type();
      String pathFromSourceDir = ((DefaultInputFile) inputFile).pathRelativeToSourceDir();
      if (pathFromSourceDir == null) {
        pathFromSourceDir = inputFile.relativePath();
      }
      Resource sonarFile = File.create(inputFile.relativePath(), pathFromSourceDir, languages.get(languageKey), unitTest);
      if ("java".equals(languageKey)) {
        sonarFile.setDeprecatedKey(JavaFile.fromRelativePath(pathFromSourceDir, false).getDeprecatedKey());
      } else {
        sonarFile.setDeprecatedKey(pathFromSourceDir);
      }
      if (sonarFile != null) {
        sonarIndex.index(sonarFile);
        importSources(fs, shouldImportSource, inputFile, sonarFile);
      }
    }
  }

  @VisibleForTesting
  void importSources(FileSystem fs, boolean shouldImportSource, InputFile inputFile, Resource sonarFile) {
    try {
      // TODO this part deserves optimization.
      // No need to read full content in memory when shouldImportSource=false
      // We should try to remove BOM and count lines in a single pass
      String source = Files.toString(inputFile.file(), fs.encoding());
      // SONAR-3860 Remove BOM character from source
      source = CharMatcher.anyOf("\uFEFF").removeFrom(source);
      fileCache.put(module.getKey(), inputFile);
      if (shouldImportSource) {
        sonarIndex.setSource(sonarFile, source);
      }
    } catch (Exception e) {
      throw new SonarException("Unable to read and import the source file : '" + inputFile.absolutePath() + "' with the charset : '"
        + fs.encoding() + "'.", e);
    }
  }
}
