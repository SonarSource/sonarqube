/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
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
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Resource;

public class DefaultFileLinesContextFactory implements FileLinesContextFactory {

  private final SonarIndex index;

  public DefaultFileLinesContextFactory(SonarIndex index) {
    this.index = index;
  }

  @Override
  public FileLinesContext createFor(Resource model) {
    // Reload resource in case it use deprecated key
    Resource resource = index.getResource(model);
    return new DefaultFileLinesContext(index, resource);
  }

  @Override
  public FileLinesContext createFor(InputFile inputFile) {
    File sonarFile = File.create(inputFile.relativePath());
    // Reload resource from index
    sonarFile = index.getResource(sonarFile);
    return new DefaultFileLinesContext(index, sonarFile);
  }

}
