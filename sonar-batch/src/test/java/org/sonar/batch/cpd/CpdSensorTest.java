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
package org.sonar.batch.cpd;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Java;
import org.sonar.batch.cpd.index.IndexFactory;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class CpdSensorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  JavaCpdEngine sonarEngine;
  DefaultCpdEngine sonarBridgeEngine;
  CpdSensor sensor;
  Settings settings;

  @Before
  public void setUp() throws IOException {
    IndexFactory indexFactory = mock(IndexFactory.class);
    sonarEngine = new JavaCpdEngine(indexFactory, null, null);
    sonarBridgeEngine = new DefaultCpdEngine(indexFactory, new CpdMappings(), null, null);
    settings = new Settings(new PropertyDefinitions(CpdComponents.class));

    DefaultFileSystem fs = new DefaultFileSystem(temp.newFolder().toPath());
    sensor = new CpdSensor(sonarEngine, sonarBridgeEngine, settings, fs);
  }

  @Test
  public void test_global_skip() {
    settings.setProperty("sonar.cpd.skip", true);
    assertThat(sensor.isSkipped(Java.KEY)).isTrue();
  }

  @Test
  public void should_not_skip_by_default() {
    assertThat(sensor.isSkipped(Java.KEY)).isFalse();
  }

  @Test
  public void should_skip_by_language() {
    settings.setProperty("sonar.cpd.skip", false);
    settings.setProperty("sonar.cpd.php.skip", true);

    assertThat(sensor.isSkipped("php")).isTrue();
    assertThat(sensor.isSkipped(Java.KEY)).isFalse();
  }

  @Test
  public void test_engine() {
    assertThat(sensor.getEngine(Java.KEY)).isSameAs(sonarEngine);
    assertThat(sensor.getEngine("PHP")).isSameAs(sonarBridgeEngine);
  }

}
