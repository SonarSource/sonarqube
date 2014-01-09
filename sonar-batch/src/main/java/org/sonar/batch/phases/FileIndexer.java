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
package org.sonar.batch.phases;

import com.google.common.base.CharMatcher;
import com.google.common.io.Files;
import org.sonar.api.BatchComponent;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.internal.InputFile;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.scan.filesystem.DefaultModuleFileSystem;

/**
 * Index all files/directories of the module in SQ database.
 * @since 4.2
 */
@InstantiationStrategy(InstantiationStrategy.PER_PROJECT)
public class FileIndexer implements BatchComponent {

  private final Project module;
  private final DefaultModuleFileSystem fs;
  private final Languages languages;
  private final Settings settings;
  private final SonarIndex sonarIndex;

  private boolean importSource;

  public FileIndexer(Project module, DefaultModuleFileSystem fs, Languages languages, SonarIndex sonarIndex, Settings settings) {
    this.module = module;
    this.fs = fs;
    this.languages = languages;
    this.sonarIndex = sonarIndex;
    this.settings = settings;
  }

  public void execute() {
    this.importSource = settings.getBoolean(CoreProperties.CORE_IMPORT_SOURCES_PROPERTY);
    String languageKey = module.getLanguageKey();
    indexFiles(fs.inputFiles(FileQuery.onSource().onLanguage(languageKey)), false, languageKey);
    indexFiles(fs.inputFiles(FileQuery.onTest().onLanguage(languageKey)), true, languageKey);
  }

  private void indexFiles(Iterable<InputFile> files, boolean unitTest, String languageKey) {
    for (InputFile inputFile : files) {
      Resource sonarFile;
      if (Java.KEY.equals(languageKey)) {
        sonarFile = JavaFile.fromRelativePath(inputFile.attribute(InputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH), unitTest);
      } else {
        File newFile = new File(languages.get(languageKey), inputFile.attribute(InputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH));
        if (newFile != null && unitTest) {
          newFile.setQualifier(Qualifiers.UNIT_TEST_FILE);
        }
        sonarFile = newFile;
      }
      if (sonarFile != null) {
        sonarFile.setPath(inputFile.path());
        sonarIndex.index(sonarFile);
        try {
          if (importSource) {
            String source = Files.toString(inputFile.file(), inputFile.encoding());
            // SONAR-3860 Remove BOM character from source
            source = CharMatcher.anyOf("\uFEFF").removeFrom(source);
            sonarIndex.setSource(sonarFile, source);
          }
        } catch (Exception e) {
          throw new SonarException("Unable to read and import the source file : '" + inputFile.absolutePath() + "' with the charset : '"
            + inputFile.encoding() + "'.", e);
        }
      }
    }
  }
}
