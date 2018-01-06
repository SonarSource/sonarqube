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
package org.sonar.api.batch.scm;

import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;

import java.util.List;

/**
 * This class should be implemented by SCM providers.
 * @since 5.0
 */
@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
@ScannerSide
public abstract class BlameCommand {

  /**
   * Compute blame of the provided files. 
   * Computation can be done in parallel if this is more efficient.
   * If there is an error that prevent to blame a file then an exception should be raised. If 
   * one file is new or contains local modifications then an exception should be raised.
   * @see BlameOutput#blameResult(InputFile, List)
   */
  public abstract void blame(BlameInput input, BlameOutput output);

  /**
   * Callback for the provider to report results of blame per file.
   */
  public interface BlameInput {

    /**
     * Filesystem of the current (sub )project.
     */
    FileSystem fileSystem();

    /**
     * List of files that should be blamed.
     */
    Iterable<InputFile> filesToBlame();

  }

  /**
   * Callback for the provider to report results of blame per file.
   */
  public interface BlameOutput {

    /**
     * Add result of the blame command for a single file. Number of lines should
     * be consistent with {@link InputFile#lines()}. This method is thread safe.
     * @param lines One entry per line in the file. <b>Every line must have a <code>non-null</code> date and revision </b>.
     */
    void blameResult(InputFile file, List<BlameLine> lines);

  }

}
