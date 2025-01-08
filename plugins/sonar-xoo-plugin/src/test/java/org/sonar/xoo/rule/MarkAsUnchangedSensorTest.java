/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.xoo.rule;

import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.xoo.Xoo;

import static org.assertj.core.api.Assertions.assertThat;

public class MarkAsUnchangedSensorTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  private final MarkAsUnchangedSensor sensor = new MarkAsUnchangedSensor();

  @Test
  public void mark_as_unchanged_for_all_files() throws IOException {
    SensorContextTester context = SensorContextTester.create(temp.newFolder());
    DefaultInputFile inputFile1 = createFile("file1");
    DefaultInputFile inputFile2 = createFile("file2");

    context.fileSystem()
      .add(inputFile1)
      .add(inputFile2);

    sensor.execute(context);
    assertThat(inputFile1.isMarkedAsUnchanged()).isTrue();
    assertThat(inputFile2.isMarkedAsUnchanged()).isTrue();
  }

  @Test
  public void only_runs_if_property_is_set() {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    sensor.describe(descriptor);
    Configuration configWithProperty = new MapSettings().setProperty("sonar.markAsUnchanged", "true").asConfig();
    Configuration configWithoutProperty = new MapSettings().asConfig();

    assertThat(descriptor.configurationPredicate().test(configWithoutProperty)).isFalse();
    assertThat(descriptor.configurationPredicate().test(configWithProperty)).isTrue();
  }

  private DefaultInputFile createFile(String name) {
    return new TestInputFileBuilder("foo", "src/" + name)
      .setLanguage(Xoo.KEY)
      .initMetadata("a\nb\nc\nd\ne\nf\ng\nh\ni\n")
      .build();
  }

}
