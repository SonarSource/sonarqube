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
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.internal.FileMetadata;
import org.sonar.api.config.Settings;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.batch.bootstrap.DefaultAnalysisMode;

@BatchSide
public class InputFileBuilderFactory {

  private final String moduleKey;
  private final PathResolver pathResolver;
  private final LanguageDetectionFactory langDetectionFactory;
  private final StatusDetectionFactory statusDetectionFactory;
  private final DefaultAnalysisMode analysisMode;
  private final Settings settings;
  private final FileMetadata fileMetadata;

  public InputFileBuilderFactory(ProjectDefinition def, PathResolver pathResolver, LanguageDetectionFactory langDetectionFactory,
    StatusDetectionFactory statusDetectionFactory, DefaultAnalysisMode analysisMode, Settings settings, FileMetadata fileMetadata) {
    this.fileMetadata = fileMetadata;
    this.moduleKey = def.getKeyWithBranch();
    this.pathResolver = pathResolver;
    this.langDetectionFactory = langDetectionFactory;
    this.statusDetectionFactory = statusDetectionFactory;
    this.analysisMode = analysisMode;
    this.settings = settings;
  }

  InputFileBuilder create(DefaultModuleFileSystem fs) {
    return new InputFileBuilder(moduleKey, pathResolver, langDetectionFactory.create(), statusDetectionFactory.create(), fs, analysisMode, settings, fileMetadata);
  }
}
