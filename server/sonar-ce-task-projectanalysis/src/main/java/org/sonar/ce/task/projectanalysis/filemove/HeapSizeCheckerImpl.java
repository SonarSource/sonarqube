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
package org.sonar.ce.task.projectanalysis.filemove;

import com.google.common.annotations.VisibleForTesting;

public class HeapSizeCheckerImpl implements HeapSizeChecker {

  private final long maxMemory;

  public HeapSizeCheckerImpl() {
    this(Runtime.getRuntime().maxMemory());
  }

  @VisibleForTesting
  HeapSizeCheckerImpl(long maxMemory) {
    this.maxMemory = maxMemory;
  }

  @Override
  public void checkHeapLimits(int totalAddedFiles, int totalRemovedFiles) {
    long matrixSize = (long) totalAddedFiles * totalRemovedFiles;
    double heapSizeInGb = toGb(maxMemory);
    double estimatedHeapNeededInGb = toGb(matrixSize * Integer.BYTES);

    if (heapSizeInGb <= estimatedHeapNeededInGb) {
      throw new TooManyFileMovesDetectedException(totalAddedFiles, totalRemovedFiles, heapSizeInGb, estimatedHeapNeededInGb);
    }
  }

  private static double toGb(long bytes) {
    return (double) bytes / 1024.0F / 1024.0F / 1024.0F;
  }

}
