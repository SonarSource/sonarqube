/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.batch;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.SonarException;

/**
 * A pre-implementation for a sensor that imports sources
 * 
 * @since 1.10
 */
@Phase(name = Phase.Name.PRE)
public abstract class AbstractSourceImporter implements Sensor {

  /**
   * @deprecated replaced by CoreProperties.CORE_IMPORT_SOURCES_PROPERTY since 1.11
   */
  @Deprecated
  public static final String KEY_IMPORT_SOURCES = "sonar.importSources";

  /**
   * @deprecated replaced by CoreProperties.CORE_IMPORT_SOURCES_DEFAULT_VALUE since 1.11
   */
  @Deprecated
  public static final boolean DEFAULT_IMPORT_SOURCES = true;

  private Language language;

  public AbstractSourceImporter(Language language) {
    this.language = language;
  }

  /**
   * {@inheritDoc}
   */
  public boolean shouldExecuteOnProject(Project project) {
    return isEnabled(project) && language.equals(project.getLanguage());
  }

  /**
   * {@inheritDoc}
   */
  public void analyse(Project project, SensorContext context) {
    analyse(project.getFileSystem(), context);
    onFinished();
  }

  protected void onFinished() {

  }

  protected void analyse(ProjectFileSystem fileSystem, SensorContext context) {
    parseDirs(context, fileSystem.getSourceFiles(language), fileSystem.getSourceDirs(), false, fileSystem.getSourceCharset());
    parseDirs(context, fileSystem.getTestFiles(language), fileSystem.getTestDirs(), true, fileSystem.getSourceCharset());
  }

  protected void parseDirs(SensorContext context, List<File> files, List<File> sourceDirs, boolean unitTest, Charset sourcesEncoding) {
    for (File file : files) {
      Resource resource = createResource(file, sourceDirs, unitTest);
      if (resource != null) {
        try {
          String source = FileUtils.readFileToString(file, sourcesEncoding.name());
          context.saveSource(resource, source);
        } catch (IOException e) {
          throw new SonarException("Unable to read and import the source file : '" + file.getAbsolutePath() + "' with the charset : '"
              + sourcesEncoding.name() + "'.", e);
        }
      }
    }
  }

  protected Resource createResource(File file, List<File> sourceDirs, boolean unitTest) {
    org.sonar.api.resources.File resource = org.sonar.api.resources.File.fromIOFile(file, sourceDirs);
    if (resource != null) {
      resource.setLanguage(language);
      if (unitTest) {
        resource.setQualifier(Resource.QUALIFIER_UNIT_TEST_CLASS);
      }
    }
    return resource;
  }

  protected boolean isEnabled(Project project) {
    return project.getConfiguration().getBoolean(CoreProperties.CORE_IMPORT_SOURCES_PROPERTY,
        CoreProperties.CORE_IMPORT_SOURCES_DEFAULT_VALUE);
  }

  /**
   * @return the language
   */
  public Language getLanguage() {
    return language;
  }
}
