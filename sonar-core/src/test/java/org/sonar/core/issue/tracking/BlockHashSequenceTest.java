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
package org.sonar.core.issue.tracking;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class BlockHashSequenceTest {

  @Test
  public void test() {
    BlockHashSequence a = new BlockHashSequence(LineHashSequence.createForLines(asList("line0", "line1", "line2")), 1);
    BlockHashSequence b = new BlockHashSequence(LineHashSequence.createForLines(asList("line0", "line1", "line2", "line3")), 1);

    assertThat(a.getBlockHashForLine(1)).isEqualTo(b.getBlockHashForLine(1));
    assertThat(a.getBlockHashForLine(2)).isEqualTo(b.getBlockHashForLine(2));
    assertThat(a.getBlockHashForLine(3)).isNotEqualTo(b.getBlockHashForLine(3));

    BlockHashSequence c = new BlockHashSequence(LineHashSequence.createForLines(asList("line-1", "line0", "line1", "line2", "line3")), 1);
    assertThat(a.getBlockHashForLine(1)).isNotEqualTo(c.getBlockHashForLine(2));
    assertThat(a.getBlockHashForLine(2)).isEqualTo(c.getBlockHashForLine(3));
  }
}
