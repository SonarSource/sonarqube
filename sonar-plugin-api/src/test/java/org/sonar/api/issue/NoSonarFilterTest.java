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
package org.sonar.api.issue;

import java.util.Arrays;
import java.util.HashSet;
import org.junit.Test;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;

import static org.assertj.core.api.Assertions.assertThat;

public class NoSonarFilterTest {

  @Test
  public void should_store_nosonar_lines_on_inputfile() {
    DefaultInputFile f = TestInputFileBuilder.create("module1", "myfile.java").setLines(8).build();
    new NoSonarFilter().noSonarInFile(f, new HashSet<>(Arrays.asList(1,4)));

     assertThat(f.hasNoSonarAt(1)).isTrue();
     assertThat(f.hasNoSonarAt(2)).isFalse();
     assertThat(f.hasNoSonarAt(4)).isTrue();
  }
}
