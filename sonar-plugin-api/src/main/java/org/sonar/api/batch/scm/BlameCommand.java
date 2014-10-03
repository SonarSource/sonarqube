/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.api.batch.scm;

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;

import java.util.List;

/**
 * @since 5.0
 */
public interface BlameCommand {

  /**
   * Compute blame of the provided files. Computation can be done in parallel.
   * If there is an error that prevent to blame a file then an exception should be raised. If 
   * one file is new or contains local modifications then an exception should be raised.
   */
  void blame(FileSystem fs, Iterable<InputFile> files, BlameResult result);

  /**
   * Callback for the provider to report results of blame per file.
   */
  public static interface BlameResult {

    /**
     * Add result of the blame command for a single file. Number of lines should
     * be consistent with {@link InputFile#lines()}.
     */
    void add(InputFile file, List<BlameLine> lines);

  }

}
