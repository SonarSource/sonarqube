/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.nio.file.Path;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.predicates.ChangedFilePredicate;
import org.sonar.api.batch.fs.internal.predicates.NonHiddenFilesPredicate;

public class MutableFileSystem extends DefaultFileSystem {

  boolean restrictToChangedFiles = false;
  boolean allowHiddenFileAnalysis = false;

  public MutableFileSystem(Path baseDir, Cache cache, FilePredicates filePredicates) {
    super(baseDir, cache, filePredicates);
  }

  MutableFileSystem(Path baseDir) {
    super(baseDir);
  }

  @Override
  public Iterable<InputFile> inputFiles(FilePredicate requestPredicate) {
    return super.inputFiles(applyAdditionalPredicate(requestPredicate));
  }

  @Override
  public InputFile inputFile(FilePredicate requestPredicate) {
    return super.inputFile(applyAdditionalPredicate(requestPredicate));
  }

  private FilePredicate applyAdditionalPredicate(FilePredicate requestPredicate) {
    return applyHiddenFilePredicate(applyChangedFilePredicate(requestPredicate));
  }

  private FilePredicate applyHiddenFilePredicate(FilePredicate predicate) {
    if (allowHiddenFileAnalysis) {
      return predicate;
    }
    return predicates().and(new NonHiddenFilesPredicate(), predicate);
  }

  private FilePredicate applyChangedFilePredicate(FilePredicate predicate) {
    if (restrictToChangedFiles) {
      return predicates().and(new ChangedFilePredicate(), predicate);
    }
    return predicate;
  }

  public void setRestrictToChangedFiles(boolean restrictToChangedFiles) {
    this.restrictToChangedFiles = restrictToChangedFiles;
  }

  public void setAllowHiddenFileAnalysis(boolean allowHiddenFileAnalysis) {
    this.allowHiddenFileAnalysis = allowHiddenFileAnalysis;
  }
}
