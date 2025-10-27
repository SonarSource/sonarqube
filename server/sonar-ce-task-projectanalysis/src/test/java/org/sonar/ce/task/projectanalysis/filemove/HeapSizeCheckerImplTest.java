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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HeapSizeCheckerImplTest {

  @Test
  void checkHeapLimits_shouldPass_whenEnoughHeap() {
    verifyPasses(1, 15000, 15000);
    verifyPasses(2, 15000, 15000);
    verifyPasses(4, 30000, 30000);
    verifyPasses(8, 40000, 40000);
    verifyPasses(16, 60000, 50000);
  }

  @Test
  void checkHeapLimits_shouldFail_whenNotEnoughHeap() {
    verifyFails(1, 18000, 18000, 1.2f);
    verifyFails(2, 25000, 22000, 2.0f);
    verifyFails(4, 35000, 36000, 4.7f);
    verifyFails(8, 46000, 48000, 8.2f);
    verifyFails(16, 70000, 70000, 18.3f);
  }

  private void verifyPasses(int heapInGb, int totalAddedFiles, int totalRemovedFiles) {
    HeapSizeCheckerImpl heapSizeChecker = new HeapSizeCheckerImpl(heapInGb * 1024L * 1024L * 1024L);
    assertThatCode(() -> heapSizeChecker.checkHeapLimits(totalAddedFiles, totalRemovedFiles)).doesNotThrowAnyException();
  }

  private void verifyFails(int heapInGb, int totalAddedFiles, int totalRemovedFiles, float estimatedHeapInGb) {
    HeapSizeCheckerImpl heapSizeChecker = new HeapSizeCheckerImpl(heapInGb * 1024L * 1024L * 1024L);
    assertThatThrownBy(() -> heapSizeChecker.checkHeapLimits(totalAddedFiles, totalRemovedFiles))
      .isInstanceOf(TooManyFileMovesDetectedException.class)
      .hasMessageContaining("Potential Out-Of-Memory (OOM) Risk Detected")
      .hasMessageContaining(totalAddedFiles + " added files and " + totalRemovedFiles + " removed files")
      .hasMessageContaining(String.format("Estimated heap needed for File Move Detection: %.1f GB", estimatedHeapInGb))
      .hasMessageContaining(String.format("Detected heap size allocated to Compute Engine: %.1f GB", heapInGb * 1.0))
    ;
  }

}
