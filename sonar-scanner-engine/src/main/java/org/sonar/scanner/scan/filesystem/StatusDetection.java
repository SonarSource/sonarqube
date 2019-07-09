/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import javax.annotation.concurrent.Immutable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.scanner.repository.FileData;
import org.sonar.scanner.repository.ProjectRepositoriesSupplier;
import org.sonar.scanner.scm.ScmChangedFiles;

import static org.sonar.api.batch.fs.InputFile.Status.ADDED;
import static org.sonar.api.batch.fs.InputFile.Status.CHANGED;
import static org.sonar.api.batch.fs.InputFile.Status.SAME;

@Immutable
public class StatusDetection {
  private final ProjectRepositoriesSupplier projectSettingsSupplier;
  private final ScmChangedFiles scmChangedFiles;

  public StatusDetection(ProjectRepositoriesSupplier projectSettingsSupplier, ScmChangedFiles scmChangedFiles) {
    this.projectSettingsSupplier = projectSettingsSupplier;
    this.scmChangedFiles = scmChangedFiles;
  }

  InputFile.Status status(String moduleKeyWithBranch, DefaultInputFile inputFile, String hash) {
    if (scmChangedFiles.isValid()) {
      return checkChangedWithScm(inputFile);
    }
    return checkChangedWithProjectRepositories(moduleKeyWithBranch, inputFile, hash);
  }

  private InputFile.Status checkChangedWithProjectRepositories(String moduleKeyWithBranch, DefaultInputFile inputFile, String hash) {
    FileData fileDataPerPath = projectSettingsSupplier.get().fileData(moduleKeyWithBranch, inputFile);
    if (fileDataPerPath == null) {
      return ADDED;
    }
    String previousHash = fileDataPerPath.hash();
    if (StringUtils.equals(hash, previousHash)) {
      return SAME;
    }
    if (StringUtils.isEmpty(previousHash)) {
      return ADDED;
    }
    return CHANGED;
  }

  private InputFile.Status checkChangedWithScm(DefaultInputFile inputFile) {
    if (!scmChangedFiles.isChanged(inputFile.path())) {
      return SAME;
    }
    return CHANGED;
  }
}
