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
package org.sonar.api.batch.sensor.duplication.internal;

import org.junit.Test;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.duplication.Duplication.Block;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultDuplicationTest {

  @Test
  public void testDuplicationEqualsAndCo() {
    DefaultInputFile file1 = new DefaultInputFile("foo", "bar.txt").setLines(50);
    DefaultInputFile file2 = new DefaultInputFile("foo", "bar2.txt").setLines(50);
    DefaultDuplication d1 = new DefaultDuplication()
      .originBlock(file1, 1, 10)
      .isDuplicatedBy(file1, 20, 29)
      .isDuplicatedBy(file2, 1, 10);
    DefaultDuplication d2 = new DefaultDuplication()
      .originBlock(file1, 1, 10)
      .isDuplicatedBy(file1, 20, 29)
      .isDuplicatedBy(file2, 1, 10);
    DefaultDuplication d3 = new DefaultDuplication()
      .originBlock(file1, 1, 10);
    assertThat(d1).isEqualTo(d1);
    assertThat(d1).isEqualTo(d2);
    assertThat(d1).isNotEqualTo("");
    assertThat(d1).isNotEqualTo(d3);

    assertThat(d1.hashCode()).isNotNull();
    assertThat(d1.toString()).contains("origin=Duplication.Block[resourceKey=foo:bar.txt,startLine=1,length=10]");
    assertThat(d1.toString()).contains(
      "duplicates=[Duplication.Block[resourceKey=foo:bar.txt,startLine=20,length=10], Duplication.Block[resourceKey=foo:bar2.txt,startLine=1,length=10]]");
  }

  @Test
  public void test() {

    DefaultInputFile file1 = new DefaultInputFile("foo", "foo.php").setLines(50);
    DefaultInputFile file2 = new DefaultInputFile("foo", "foo2.php").setLines(50);

    DefaultDuplication dup = new DefaultDuplication().originBlock(file1, 1, 11)
      .isDuplicatedBy(file1, 40, 50)
      .isDuplicatedBy(file2, 1, 10);

    Block originBlock = dup.originBlock();
    assertThat(originBlock.resourceKey()).isEqualTo("foo:foo.php");
    assertThat(originBlock.startLine()).isEqualTo(1);
    assertThat(originBlock.length()).isEqualTo(11);
    assertThat(dup.duplicates()).hasSize(2);
  }

}
