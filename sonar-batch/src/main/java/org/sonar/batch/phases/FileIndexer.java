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

import org.sonar.api.BatchComponent;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.internal.InputFile;
import org.sonar.batch.scan.filesystem.DefaultModuleFileSystem;

/**
 * Index all files/directories of the module in SQ database.
 * @since 4.2
 */
@InstantiationStrategy(InstantiationStrategy.PER_PROJECT)
public class FileIndexer implements BatchComponent {

  private Project module;
  private DefaultModuleFileSystem fs;

  private Languages languages;

  public FileIndexer(Project module, DefaultModuleFileSystem fs, Languages languages) {
    this.module = module;
    this.fs = fs;
    this.languages = languages;
  }

  public void execute(SensorContext context) {
    String languageKey = module.getLanguageKey();
    indexFiles(fs.inputFiles(FileQuery.onSource().onLanguage(languageKey)), false, context, languageKey);
    indexFiles(fs.inputFiles(FileQuery.onTest().onLanguage(languageKey)), true, context, languageKey);
  }

  private void indexFiles(Iterable<InputFile> files, boolean unitTest, SensorContext context, String languageKey) {
    for (InputFile inputFile : files) {
      Resource sonarFile;
      if (Java.KEY.equals(languageKey)) {
        sonarFile = JavaFile.fromRelativePath(inputFile.attribute(InputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH), unitTest);
      } else {
        sonarFile = new org.sonar.api.resources.File(languages.get(languageKey),
          inputFile.attribute(InputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH));
      }
      if (sonarFile != null) {
        sonarFile.setPath(inputFile.path());
        context.index(sonarFile);
      }
    }
  }
}
