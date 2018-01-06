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
package org.sonar.duplications.detector.original;

import java.util.Arrays;
import java.util.List;

import org.sonar.duplications.block.Block;
import org.sonar.duplications.detector.DetectorTestCase;
import org.sonar.duplications.index.CloneGroup;
import org.sonar.duplications.index.CloneIndex;

public class OriginalCloneDetectionAlgorithmTest extends DetectorTestCase {

  @Override
  protected List<CloneGroup> detect(CloneIndex index, Block[] fileBlocks) {
    return OriginalCloneDetectionAlgorithm.detect(index, Arrays.asList(fileBlocks));
  }

}
