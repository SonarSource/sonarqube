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
package org.sonar.api.batch.sensor.code.internal;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.code.internal.DefaultSignificantCode;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DefaultSignificantCodeTest {
  private SensorStorage sensorStorage = mock(SensorStorage.class);
  private DefaultSignificantCode underTest = new DefaultSignificantCode(sensorStorage);
  private InputFile inputFile = TestInputFileBuilder.create("module", "file1.xoo")
    .setContents("this is\na file\n with some code")
    .build();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void should_save_ranges() {
    underTest.onFile(inputFile)
      .addRange(inputFile.selectLine(1))
      .save();
    verify(sensorStorage).store(underTest);
  }

  @Test
  public void fail_if_save_without_file() {
    exception.expect(IllegalStateException.class);
    exception.expectMessage("Call onFile() first");
    underTest.save();
  }

  @Test
  public void fail_if_add_range_to_same_line_twice() {
    underTest.onFile(inputFile);
    underTest.addRange(inputFile.selectLine(1));

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Significant code was already reported for line '1'.");
    underTest.addRange(inputFile.selectLine(1));
  }

  @Test
  public void fail_if_range_includes_many_lines() {
    underTest.onFile(inputFile);

    exception.expect(IllegalArgumentException.class);
    exception.expectMessage("Ranges of significant code must be located in a single line");
    underTest.addRange(inputFile.newRange(1, 1, 2, 1));
  }

  @Test
  public void fail_if_add_range_before_setting_file() {
    exception.expect(IllegalStateException.class);
    exception.expectMessage("addRange() should be called after on()");
    underTest.addRange(inputFile.selectLine(1));
  }
}
