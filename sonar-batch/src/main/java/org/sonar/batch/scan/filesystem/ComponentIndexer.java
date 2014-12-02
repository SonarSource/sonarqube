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

import org.sonar.api.BatchComponent;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DeprecatedDefaultInputFile;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.batch.index.ResourceKeyMigration;
import org.sonar.batch.util.DeprecatedKeyUtils;

/**
 * Index all files/directories of the module in SQ database and importing source code.
 *
 * @since 4.2
 */
public class ComponentIndexer implements BatchComponent {

  private final Languages languages;
  private final SonarIndex sonarIndex;
  private final ResourceKeyMigration migration;
  private final Project module;

  public ComponentIndexer(Project module, Languages languages, SonarIndex sonarIndex, ResourceKeyMigration migration) {
    this.module = module;
    this.languages = languages;
    this.sonarIndex = sonarIndex;
    this.migration = migration;
  }

  public void execute(FileSystem fs) {
    migration.migrateIfNeeded(module, fs);

    for (InputFile inputFile : fs.inputFiles(fs.predicates().all())) {
      String languageKey = inputFile.language();
      boolean unitTest = InputFile.Type.TEST == inputFile.type();
      String pathFromSourceDir = ((DeprecatedDefaultInputFile) inputFile).pathRelativeToSourceDir();
      if (pathFromSourceDir == null) {
        pathFromSourceDir = inputFile.relativePath();
      }
      Resource sonarFile = File.create(inputFile.relativePath(), pathFromSourceDir, languages.get(languageKey), unitTest);
      if ("java".equals(languageKey)) {
        sonarFile.setDeprecatedKey(DeprecatedKeyUtils.getJavaFileDeprecatedKey(pathFromSourceDir));
      } else {
        sonarFile.setDeprecatedKey(pathFromSourceDir);
      }
      sonarIndex.index(sonarFile);
    }
  }

}
