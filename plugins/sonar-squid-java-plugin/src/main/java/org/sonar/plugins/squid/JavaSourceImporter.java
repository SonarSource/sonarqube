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
package org.sonar.plugins.squid;

import org.apache.commons.io.FileUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.*;
import org.sonar.api.resources.*;
import org.sonar.api.utils.SonarException;
import org.sonar.java.api.JavaUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

@Phase(name = Phase.Name.PRE)
@DependsUpon(classes = SquidSensor.class)
@DependedUpon(JavaUtils.BARRIER_AFTER_SQUID)
public final class JavaSourceImporter implements Sensor {

  /**
   * {@inheritDoc}
   */
  public boolean shouldExecuteOnProject(Project project) {
    return isEnabled(project) && Java.KEY.equals(project.getLanguageKey());
  }

  /**
   * {@inheritDoc}
   */
  public void analyse(Project project, SensorContext context) {
    analyse(project.getFileSystem(), context);
  }

  void analyse(ProjectFileSystem fileSystem, SensorContext context) {
    parseDirs(context, fileSystem.mainFiles(Java.INSTANCE), false, fileSystem.getSourceCharset());
    parseDirs(context, fileSystem.testFiles(Java.INSTANCE), true, fileSystem.getSourceCharset());
  }

  void parseDirs(SensorContext context, List<InputFile> inputFiles, boolean unitTest, Charset sourcesEncoding) {
    for (InputFile inputFile : inputFiles) {
      JavaFile javaFile = JavaFile.fromRelativePath(inputFile.getRelativePath(), unitTest);
      importSource(context, javaFile, inputFile, sourcesEncoding);
    }
  }

  void importSource(SensorContext context, JavaFile javaFile, InputFile inputFile, Charset sourcesEncoding) {
    try {
      //if (!context.isIndexed(javaFile, true)) {
      // See http://jira.codehaus.org/browse/SONAR-791
      // Squid is the reference plugin to index files. If a file is not indexed,
      //  throw new SonarException("Invalid file: " + javaFile + ". Please check that Java source directories match root directories" +
      //    " as defined by packages.");
      //}
      String source = FileUtils.readFileToString(inputFile.getFile(), sourcesEncoding.name());
      context.saveSource(javaFile, source);

    } catch (IOException e) {
      throw new SonarException("Unable to read and import the source file : '" + inputFile.getFile().getAbsolutePath() + "' with the charset : '"
          + sourcesEncoding.name() + "'.", e);
    }
  }

  boolean isEnabled(Project project) {
    return project.getConfiguration().getBoolean(CoreProperties.CORE_IMPORT_SOURCES_PROPERTY,
        CoreProperties.CORE_IMPORT_SOURCES_DEFAULT_VALUE);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

}
