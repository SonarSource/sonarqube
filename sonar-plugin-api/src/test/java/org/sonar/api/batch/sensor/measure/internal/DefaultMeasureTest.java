/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.api.batch.sensor.measure.internal;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.measures.CoreMetrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DefaultMeasureTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void build_file_measure() {
    SensorStorage storage = mock(SensorStorage.class);
    DefaultMeasure<Integer> newMeasure = new DefaultMeasure<Integer>(storage)
      .forMetric(CoreMetrics.LINES)
      .on(new TestInputFileBuilder("foo", "src/Foo.php").build())
      .withValue(3);

    assertThat(newMeasure.inputComponent()).isEqualTo(new TestInputFileBuilder("foo", "src/Foo.php").build());
    assertThat(newMeasure.metric()).isEqualTo(CoreMetrics.LINES);
    assertThat(newMeasure.value()).isEqualTo(3);

    newMeasure.save();

    verify(storage).store(newMeasure);
  }

  @Test
  public void build_project_measure() {
    SensorStorage storage = mock(SensorStorage.class);
    DefaultInputModule module = new DefaultInputModule("foo");
    DefaultMeasure<Integer> newMeasure = new DefaultMeasure<Integer>(storage)
      .forMetric(CoreMetrics.LINES)
      .on(module)
      .withValue(3);

    assertThat(newMeasure.inputComponent()).isEqualTo(new DefaultInputModule("foo"));
    assertThat(newMeasure.metric()).isEqualTo(CoreMetrics.LINES);
    assertThat(newMeasure.value()).isEqualTo(3);

    newMeasure.save();

    verify(storage).store(newMeasure);
  }

  @Test
  public void not_allowed_to_call_on_twice() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("on() already called");
    new DefaultMeasure<Integer>()
      .on(new DefaultInputModule("foo"))
      .on(new TestInputFileBuilder("foo", "src/Foo.php").build())
      .withValue(3)
      .save();
  }

}
