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
import org.sonar.api.batch.sensor.duplication.DuplicationGroup;
import org.sonar.api.batch.sensor.duplication.DuplicationGroup.Block;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultDuplicationBuilderTest {

  @Test
  public void test() {
    DefaultDuplicationBuilder builder = new DefaultDuplicationBuilder(new DefaultInputFile("foo", "foo.php"));

    List<DuplicationGroup> duplicationGroup = builder.originBlock(1, 11)
      .isDuplicatedBy(new DefaultInputFile("foo", "foo.php"), 40, 50)
      .isDuplicatedBy(new DefaultInputFile("foo", "foo2.php"), 1, 10)
      .originBlock(20, 30)
      .isDuplicatedBy(new DefaultInputFile("foo", "foo3.php"), 30, 40)
      .build();

    assertThat(duplicationGroup).hasSize(2);
    Block originBlock = duplicationGroup.get(0).originBlock();
    assertThat(originBlock.resourceKey()).isEqualTo("foo:foo.php");
    assertThat(originBlock.startLine()).isEqualTo(1);
    assertThat(originBlock.length()).isEqualTo(11);
    assertThat(duplicationGroup.get(0).duplicates()).hasSize(2);
  }

}
