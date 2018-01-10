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
package org.sonar.server.computation.task.projectanalysis.source;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.db.protobuf.DbFileSources;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

public class ComputeFileSourceDataTest {

  @Test
  public void compute_one_line() {
    ComputeFileSourceData computeFileSourceData = new ComputeFileSourceData(
      newArrayList("line1").iterator(),
      Lists.newArrayList(new MockLineReader()),
      1);

    ComputeFileSourceData.Data data = computeFileSourceData.compute();
    assertThat(data.getLineHashes()).isEqualTo("137f72c3708c6bd0de00a0e5a69c699b");
    assertThat(data.getSrcHash()).isEqualTo("137f72c3708c6bd0de00a0e5a69c699b");
    assertThat(data.getFileSourceData().getLinesList()).hasSize(1);
    assertThat(data.getFileSourceData().getLines(0).getHighlighting()).isEqualTo("h-1");
  }

  @Test
  public void compute_two_lines() {
    ComputeFileSourceData computeFileSourceData = new ComputeFileSourceData(
      newArrayList("line1", "line2").iterator(),
      Lists.newArrayList(new MockLineReader()),
      2);

    ComputeFileSourceData.Data data = computeFileSourceData.compute();
    assertThat(data.getLineHashes()).isEqualTo("137f72c3708c6bd0de00a0e5a69c699b\ne6251bcf1a7dc3ba5e7933e325bbe605");
    assertThat(data.getSrcHash()).isEqualTo("ee5a58024a155466b43bc559d953e018");
    assertThat(data.getFileSourceData().getLinesList()).hasSize(2);
    assertThat(data.getFileSourceData().getLines(0).getHighlighting()).isEqualTo("h-1");
    assertThat(data.getFileSourceData().getLines(1).getHighlighting()).isEqualTo("h-2");
  }

  @Test
  public void remove_tabs_and_spaces_in_line_hashes() {
    String refLineHashes = new ComputeFileSourceData(
      newArrayList("line1").iterator(),
      Lists.newArrayList(new MockLineReader()),
      1).compute().getLineHashes();

    assertThat(new ComputeFileSourceData(
      newArrayList(" line\t \t 1  ").iterator(),
      Lists.newArrayList(new MockLineReader()),
      1).compute().getLineHashes()).isEqualTo(refLineHashes);
  }

  @Test
  public void compute_line_hashes_of_empty_lines() {
    assertThat(new ComputeFileSourceData(
      newArrayList("   ").iterator(),
      Lists.newArrayList(new MockLineReader()),
      1).compute().getLineHashes()).isEqualTo("");
  }

  private static class MockLineReader implements LineReader {
    @Override
    public void read(DbFileSources.Line.Builder lineBuilder) {
      lineBuilder.setHighlighting("h-" + lineBuilder.getLine());
    }
  }
}
