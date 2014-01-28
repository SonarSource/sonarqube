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
package org.sonar.batch;

import org.sonar.api.batch.SonarIndex;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Resource;
import org.sonar.api.scan.filesystem.InputFile;
import org.sonar.api.scan.filesystem.internal.DefaultInputFile;

public class DefaultFileLinesContextFactory implements FileLinesContextFactory {

  private final SonarIndex index;
  private Languages languages;

  public DefaultFileLinesContextFactory(SonarIndex index, Languages languages) {
    this.index = index;
    this.languages = languages;
  }

  @Override
  public FileLinesContext createFor(Resource resource) {
    // Reload resource in case it use deprecated key
    resource = index.getResource(resource);
    return new DefaultFileLinesContext(index, resource);
  }

  @Override
  public FileLinesContext createFor(InputFile inputFile) {
    // FIXME remove that once DefaultFileLinesContext accept an InputFile
    String languageKey = inputFile.attribute(InputFile.ATTRIBUTE_LANGUAGE);
    boolean unitTest = InputFile.TYPE_TEST.equals(inputFile.attribute(InputFile.ATTRIBUTE_TYPE));
    Resource sonarFile;
    if (Java.KEY.equals(languageKey)) {
      sonarFile = JavaFile.create(inputFile.path(), inputFile.attribute(DefaultInputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH), unitTest);
    } else {
      sonarFile = File.create(inputFile.path(), inputFile.attribute(DefaultInputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH), languages.get(languageKey), unitTest);
    }
    return new DefaultFileLinesContext(index, sonarFile);
  }

}
