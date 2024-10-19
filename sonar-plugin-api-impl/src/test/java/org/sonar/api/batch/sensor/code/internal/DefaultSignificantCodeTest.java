/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.api.batch.sensor.code.internal;

import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorStorage;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DefaultSignificantCodeTest {
  private SensorStorage sensorStorage = mock(SensorStorage.class);
  private DefaultSignificantCode underTest = new DefaultSignificantCode(sensorStorage);
  private InputFile inputFile = TestInputFileBuilder.create("module", "file1.xoo")
    .setContents("this is\na file\n with some code")
    .build();

  @Test
  public void should_save_ranges() {
    underTest.onFile(inputFile)
      .addRange(inputFile.selectLine(1))
      .save();
    verify(sensorStorage).store(underTest);
  }

  @Test
  public void fail_if_save_without_file() {
    assertThatThrownBy(() -> underTest.save())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Call onFile() first");
  }

  @Test
  public void fail_if_add_range_to_same_line_twice() {
    underTest.onFile(inputFile);
    underTest.addRange(inputFile.selectLine(1));

    assertThatThrownBy(() -> underTest.addRange(inputFile.selectLine(1)))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Significant code was already reported for line '1'.");
  }

  @Test
  public void fail_if_range_includes_many_lines() {
    underTest.onFile(inputFile);

    assertThatThrownBy(() -> underTest.addRange(inputFile.newRange(1, 1, 2, 1)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Ranges of significant code must be located in a single line");
  }

  @Test
  public void fail_if_add_range_before_setting_file() {
    assertThatThrownBy(() -> underTest.addRange(inputFile.selectLine(1)))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("addRange() should be called after on()");
  }
}
