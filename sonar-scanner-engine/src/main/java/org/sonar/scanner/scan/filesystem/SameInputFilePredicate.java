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

import java.util.function.Predicate;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Status;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.OperatorPredicate;
import org.sonar.api.batch.fs.internal.StatusPredicate;

public class SameInputFilePredicate implements Predicate<InputFile> {
  private final StatusDetection statusDetection;
  private final String moduleKeyWithBranch;
  private final FilePredicate currentPredicate;

  public SameInputFilePredicate(FilePredicate currentPredicate, StatusDetection statusDetection, String moduleKeyWithBranch) {
    this.currentPredicate = currentPredicate;
    this.statusDetection = statusDetection;
    this.moduleKeyWithBranch = moduleKeyWithBranch;
  }

  @Override
  public boolean test(InputFile inputFile) {
    if (hasExplicitFilterOnStatus(currentPredicate)) {
      // If user explicitly requested a given status, don't change the result
      return true;
    }

    // TODO: the inputFile could try to calculate the status itself without generating metadata
    Status status = statusDetection.getStatusWithoutMetadata(moduleKeyWithBranch, (DefaultInputFile) inputFile);
    if (status != null) {
      return status != Status.SAME;
    }

    // this will trigger computation of metadata
    return inputFile.status() != Status.SAME;
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
