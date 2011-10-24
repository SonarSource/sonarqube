/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import org.apache.commons.io.FileUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.resources.*;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

/**
 * A pre-implementation for a sensor that imports sources.
 * It became too much ugly because of extensability. Methods can't be
 * refactored because they are heavily overridden in plugins.
 *
 * @since 1.10
 */
@Phase(name = Phase.Name.PRE)
public abstract class AbstractSourceImporter implements Sensor {

  private Language language;
  private boolean enabled = false;

  public AbstractSourceImporter(Language language) {
    this.language = language;
  }

  /**
   * {@inheritDoc}
   */
  public boolean shouldExecuteOnProject(Project project) {
    return language.equals(project.getLanguage());
  }

  /**
   * {@inheritDoc}
   */
  public void analyse(Project project, SensorContext context) {
    enabled = isEnabled(project);
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
          context.index(resource);
          if (enabled) {
            String source = FileUtils.readFileToString(file, sourcesEncoding.name());
            context.saveSource(resource, source);
          }
        } catch (Exception e) {
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
        resource.setQualifier(Qualifiers.UNIT_TEST_FILE);
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
