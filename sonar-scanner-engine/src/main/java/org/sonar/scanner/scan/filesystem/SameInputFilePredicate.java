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
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.OperatorPredicate;
import org.sonar.api.batch.fs.internal.StatusPredicate;
import org.sonar.scanner.repository.FileData;
import org.sonar.scanner.repository.ProjectRepositories;

public class SameInputFilePredicate implements Predicate<InputFile> {
  private final ProjectRepositories projectRepositories;
  private final String moduleKeyWithBranch;
  private final FilePredicate currentPredicate;

  public SameInputFilePredicate(FilePredicate currentPredicate, ProjectRepositories projectRepositories, String moduleKeyWithBranch) {
    this.currentPredicate = currentPredicate;
    this.projectRepositories = projectRepositories;
    this.moduleKeyWithBranch = moduleKeyWithBranch;
  }

  @Override
  public boolean test(InputFile inputFile) {
    if (hasExplicitFilterOnStatus(currentPredicate)) {
      // If user explicitely requested a given status, don't change the result
      return true;
    }
    // Try to avoid initializing metadata
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
    return inputFile.status() != InputFile.Status.SAME;
  }

  static boolean hasExplicitFilterOnStatus(FilePredicate predicate) {
    if (predicate instanceof StatusPredicate) {
      return true;
    }
    if (predicate instanceof OperatorPredicate) {
      return ((OperatorPredicate) predicate).operands().stream().anyMatch(SameInputFilePredicate::hasExplicitFilterOnStatus);
    }
    return false;
  }

}
