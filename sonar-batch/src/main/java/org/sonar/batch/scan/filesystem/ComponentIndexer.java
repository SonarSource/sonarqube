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

import org.sonar.api.BatchSide;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.batch.index.ResourceCache;
import org.sonar.batch.index.ResourceKeyMigration;
import org.sonar.batch.index.ResourcePersister;

import javax.annotation.Nullable;

/**
 * Index all files/directories of the module in SQ database and importing source code.
 *
 * @since 4.2
 */
@BatchSide
public class ComponentIndexer {

  private final Languages languages;
  private final SonarIndex sonarIndex;
  private final ResourceKeyMigration migration;
  private final Project module;
  private final ResourcePersister resourcePersister;
  private final ResourceCache resourceCache;

  public ComponentIndexer(Project module, Languages languages, SonarIndex sonarIndex, ResourceCache resourceCache, @Nullable ResourceKeyMigration migration,
    @Nullable ResourcePersister resourcePersister) {
    this.module = module;
    this.languages = languages;
    this.sonarIndex = sonarIndex;
    this.resourceCache = resourceCache;
    this.migration = migration;
    this.resourcePersister = resourcePersister;
  }

  public ComponentIndexer(Project module, Languages languages, SonarIndex sonarIndex, ResourceCache resourceCache) {
    this(module, languages, sonarIndex, resourceCache, null, null);
  }

  public void execute(DefaultModuleFileSystem fs) {
    module.setBaseDir(fs.baseDir());

    if (resourcePersister != null) {
      // Force persistence of module structure in order to know if project should be migrated
      resourcePersister.persist();
    }

    if (migration != null) {
      migration.migrateIfNeeded(module, fs);
    }

    for (InputFile inputFile : fs.inputFiles(fs.predicates().all())) {
      String languageKey = inputFile.language();
      boolean unitTest = InputFile.Type.TEST == inputFile.type();
      Resource sonarFile = File.create(inputFile.relativePath(), languages.get(languageKey), unitTest);
      sonarIndex.index(sonarFile);
      resourceCache.get(sonarFile).setInputPath(inputFile);
    }

    if (resourcePersister != null) {
      // Persist all files in order to have snapshot availables
      resourcePersister.persist();
    }
  }
}
