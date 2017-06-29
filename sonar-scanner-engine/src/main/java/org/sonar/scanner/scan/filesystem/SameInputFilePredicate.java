/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.scanner.scan.filesystem;

import java.util.function.Predicate;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.scanner.repository.FileData;
import org.sonar.scanner.repository.ProjectRepositories;

public class SameInputFilePredicate implements Predicate<InputFile> {
  private static final Logger LOG = LoggerFactory.getLogger(SameInputFilePredicate.class);
  private final ProjectRepositories projectRepositories;
  private final String moduleKeyWithBranch;

  public SameInputFilePredicate(ProjectRepositories projectRepositories, String moduleKeyWithBranch) {
    this.projectRepositories = projectRepositories;
    this.moduleKeyWithBranch = moduleKeyWithBranch;
  }

  @Override
  public boolean test(InputFile inputFile) {
    FileData fileDataPerPath = projectRepositories.fileData(moduleKeyWithBranch, inputFile.relativePath());
    if (fileDataPerPath == null) {
      // ADDED
      return true;
    }
    String previousHash = fileDataPerPath.hash();
    if (StringUtils.isEmpty(previousHash)) {
      // ADDED
      return true;
    }

    // this will trigger computation of metadata
    String hash = ((DefaultInputFile) inputFile).hash();
    if (StringUtils.equals(hash, previousHash)) {
      // SAME
      LOG.debug("'{}' filtering unmodified file", inputFile.relativePath());
      return false;
    }

    // CHANGED
    return true;
  }

}
