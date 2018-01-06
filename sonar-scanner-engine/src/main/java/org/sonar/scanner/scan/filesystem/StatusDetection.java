/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.scanner.repository.FileData;
import org.sonar.scanner.repository.ProjectRepositories;
import org.sonar.scanner.scm.ScmChangedFiles;

import static org.sonar.api.batch.fs.InputFile.Status.*;

@Immutable
public class StatusDetection {

  private final ProjectRepositories projectRepositories;
  private final ScmChangedFiles scmChangedFiles;

  public StatusDetection(ProjectRepositories projectSettings, ScmChangedFiles scmChangedFiles) {
    this.projectRepositories = projectSettings;
    this.scmChangedFiles = scmChangedFiles;
  }

  InputFile.Status status(String projectKeyWithBranch, DefaultInputFile inputFile, String hash) {
    FileData fileDataPerPath = projectRepositories.fileData(projectKeyWithBranch, inputFile.relativePath());
    if (fileDataPerPath == null) {
      return checkChanged(ADDED, inputFile);
    }
    String previousHash = fileDataPerPath.hash();
    if (StringUtils.equals(hash, previousHash)) {
      return SAME;
    }
    if (StringUtils.isEmpty(previousHash)) {
      return checkChanged(ADDED, inputFile);
    }
    return checkChanged(CHANGED, inputFile);
  }

  /**
   * If possible, get the status of the provided file without initializing metadata of the file.
   * @return null if it was not possible to get the status without calculating metadata
   */
  @CheckForNull
  public InputFile.Status getStatusWithoutMetadata(String projectKeyWithBranch, DefaultInputFile inputFile) {
    FileData fileDataPerPath = projectRepositories.fileData(projectKeyWithBranch, inputFile.relativePath());
    if (fileDataPerPath == null) {
      return checkChanged(ADDED, inputFile);
    }
    String previousHash = fileDataPerPath.hash();
    if (StringUtils.isEmpty(previousHash)) {
      return checkChanged(ADDED, inputFile);
    }
    return null;
  }

  private InputFile.Status checkChanged(InputFile.Status status, DefaultInputFile inputFile) {
    if (!scmChangedFiles.verifyChanged(inputFile.path())) {
      return SAME;
    }
    return status;
  }
}
