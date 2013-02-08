/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

import com.google.common.base.CharMatcher;
import com.google.common.io.Files;
import org.sonar.api.CoreProperties;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
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
   * Generally this method should not be overridden in subclasses, but if it is, then it should be executed anyway (see SONAR-3419).
   */
  public boolean shouldExecuteOnProject(Project project) {
    enabled = isEnabled(project);
    return language.equals(project.getLanguage());
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
          context.index(resource);
          if (enabled) {
            String source = Files.toString(file, Charset.forName(sourcesEncoding.name()));
            // SONAR-3860 Remove Bom character from source
            source = CharMatcher.anyOf("\uFEFF").removeFrom(source);
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
