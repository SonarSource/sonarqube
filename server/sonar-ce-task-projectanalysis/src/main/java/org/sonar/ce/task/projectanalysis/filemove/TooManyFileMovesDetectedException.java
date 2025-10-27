/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.filemove;

public class TooManyFileMovesDetectedException extends RuntimeException {

  private static final String DEFAULT_MESSAGE = """
    Analysis Failed.
    ---------------------
    REASON: Potential Out-Of-Memory (OOM) Risk Detected.
    The Compute Engine detected an excessively large volume of potential file moves: %d added files and %d removed files.
    - Estimated heap needed for File Move Detection: %.1f GB.
    - Detected heap size allocated to Compute Engine: %.1f GB.
    The scan failed proactively to avoid an out-of-memory failure on the server.
    
    NEXT STEPS (Choose One):
    
      1. Unintended Move (Mistake):
    Please revert the project structure to the previous commit and re-run the analysis.
    
      2. Intended Move (Legitimate Restructure):
    The volume of moved/renamed files is too large to be processed. Please restore the old structure, and perform this move in multiple batches, each followed by analysis.
    ---------------------
    """;

  public TooManyFileMovesDetectedException(int addedFilesCount, int removedFilesCount, double detectedHeapInGB, double estimatedHeapNeededInGB) {
    super(String.format(DEFAULT_MESSAGE, addedFilesCount, removedFilesCount, estimatedHeapNeededInGB, detectedHeapInGB));
  }
}
