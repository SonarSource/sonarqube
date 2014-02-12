/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
import org.sonar.api.config.Settings;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.InputFile;
import org.sonar.api.scan.filesystem.internal.DefaultInputFile;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.index.ResourceKeyMigration;
import org.sonar.batch.scan.language.DefaultModuleLanguages;
import org.sonar.core.resource.ResourceDao;

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
  private final DefaultModuleLanguages moduleLanguages;
  private final ResourceDao resourceDao;
  private InputFileCache fileCache;

  public ComponentIndexer(Project module, Languages languages, SonarIndex sonarIndex, Settings settings, ResourceKeyMigration migration,
    DefaultModuleLanguages moduleLanguages, ResourceDao resourceDao, InputFileCache fileCache) {
    this.module = module;
    this.languages = languages;
    this.sonarIndex = sonarIndex;
    this.settings = settings;
    this.migration = migration;
    this.moduleLanguages = moduleLanguages;
    this.resourceDao = resourceDao;
    this.fileCache = fileCache;
  }

  public void execute(DefaultModuleFileSystem fs) {
    boolean shouldImportSource = settings.getBoolean(CoreProperties.CORE_IMPORT_SOURCES_PROPERTY);
    Iterable<InputFile> inputFiles = fs.inputFiles(FileQuery.all());
    migration.migrateIfNeeded(module, inputFiles);
    for (InputFile inputFile : inputFiles) {
      String languageKey = inputFile.attribute(InputFile.ATTRIBUTE_LANGUAGE);
      boolean unitTest = InputFile.TYPE_TEST.equals(inputFile.attribute(InputFile.ATTRIBUTE_TYPE));
      String pathFromSourceDir = inputFile.attribute(DefaultInputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH);
      if (pathFromSourceDir == null) {
        pathFromSourceDir = inputFile.path();
      }
      Resource sonarFile = File.create(inputFile.path(), pathFromSourceDir, languages.get(languageKey), unitTest);
      if (Java.KEY.equals(languageKey)) {
        sonarFile.setDeprecatedKey(JavaFile.fromRelativePath(pathFromSourceDir, false).getDeprecatedKey());
      } else {
        sonarFile.setDeprecatedKey(pathFromSourceDir);
      }
      if (sonarFile != null) {
        moduleLanguages.addLanguage(languageKey);
        sonarIndex.index(sonarFile);
        importSources(shouldImportSource, inputFile, sonarFile);
      }
    }
  }

  @VisibleForTesting
  void importSources(boolean shouldImportSource, InputFile inputFile, Resource sonarFile) {
    try {
      // TODO this part deserve optimization.
      // No need to read full content in memory when shouldImportSource=false
      // We should try to remove BOM and count lines in a single pass
      String source = Files.toString(inputFile.file(), inputFile.encoding());
      // SONAR-3860 Remove BOM character from source
      source = CharMatcher.anyOf("\uFEFF").removeFrom(source);
      String[] lines = source.split("(\r)?\n|\r", -1);
      inputFile.attributes().put(InputFile.ATTRIBUTE_LINE_COUNT, String.valueOf(lines.length));
      fileCache.put(module.getKey(), inputFile);
      if (shouldImportSource) {
        sonarIndex.setSource(sonarFile, source);
      }
    } catch (Exception e) {
      throw new SonarException("Unable to read and import the source file : '" + inputFile.absolutePath() + "' with the charset : '"
        + inputFile.encoding() + "'.", e);
    }
  }
}
