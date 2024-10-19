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
package org.sonar.api.batch.sensor.measure.internal;

import java.io.IOException;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.internal.AbstractProjectOrModule;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.measures.CoreMetrics;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DefaultMeasureTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void build_file_measure() {
    SensorStorage storage = Mockito.mock(SensorStorage.class);
    DefaultMeasure<Integer> newMeasure = new DefaultMeasure<Integer>(storage)
      .forMetric(CoreMetrics.LINES)
      .on(new TestInputFileBuilder("foo", "src/Foo.php").build())
      .withValue(3);

    Assertions.assertThat(newMeasure.inputComponent()).isEqualTo(new TestInputFileBuilder("foo", "src/Foo.php").build());
    Assertions.assertThat(newMeasure.metric()).isEqualTo(CoreMetrics.LINES);
    Assertions.assertThat(newMeasure.value()).isEqualTo(3);

    newMeasure.save();

    Mockito.verify(storage).store(newMeasure);
  }

  @Test
  public void build_project_measure() throws IOException {
    SensorStorage storage = Mockito.mock(SensorStorage.class);
    AbstractProjectOrModule module = new DefaultInputProject(ProjectDefinition.create().setKey("foo").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder()));
    DefaultMeasure<Integer> newMeasure = new DefaultMeasure<Integer>(storage)
      .forMetric(CoreMetrics.LINES)
      .on(module)
      .withValue(3);

    Assertions.assertThat(newMeasure.inputComponent()).isEqualTo(module);
    Assertions.assertThat(newMeasure.metric()).isEqualTo(CoreMetrics.LINES);
    Assertions.assertThat(newMeasure.value()).isEqualTo(3);

    newMeasure.save();

    Mockito.verify(storage).store(newMeasure);
  }

  @Test
  public void not_allowed_to_call_on_twice() {
    assertThatThrownBy(() -> new DefaultMeasure<Integer>()
      .on(new DefaultInputProject(ProjectDefinition.create().setKey("foo").setBaseDir(temp.newFolder()).setWorkDir(temp.newFolder())))
      .on(new TestInputFileBuilder("foo", "src/Foo.php").build())
      .withValue(3)
      .save())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("on() already called");
  }

}
