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
package org.sonar.api.batch.fs.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class MetadataTest {
  @Test
  public void testRoundtrip() {
    Metadata metadata = new Metadata(10, 20, "hash", new int[] {1, 3}, new int[] {2, 4}, 5);
    assertThat(metadata.isEmpty()).isFalse();
    assertThat(metadata.lines()).isEqualTo(10);
    assertThat(metadata.nonBlankLines()).isEqualTo(20);
    assertThat(metadata.originalLineStartOffsets()).isEqualTo(new int[] {1, 3});
    assertThat(metadata.originalLineEndOffsets()).isEqualTo(new int[] {2, 4});
    assertThat(metadata.lastValidOffset()).isEqualTo(5);
    assertThat(metadata.hash()).isEqualTo("hash");
  }
}
