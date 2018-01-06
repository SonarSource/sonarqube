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
package org.sonar.scanner.cpd.deprecated;

import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.scanner.FakeJava;

import static org.assertj.core.api.Assertions.assertThat;

public class DeprecatedCpdBlockIndexerSensorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  JavaCpdBlockIndexer sonarEngine;
  DefaultCpdBlockIndexer sonarBridgeEngine;
  DeprecatedCpdBlockIndexerSensor sensor;

  @Before
  public void setUp() throws IOException {
    sonarEngine = new JavaCpdBlockIndexer(null, null, null);
    sonarBridgeEngine = new DefaultCpdBlockIndexer(new CpdMappings(), null, null, null);

    DefaultFileSystem fs = new DefaultFileSystem(temp.newFolder().toPath());
    sensor = new DeprecatedCpdBlockIndexerSensor(sonarEngine, sonarBridgeEngine, fs);
  }

  @Test
  public void test_engine() {
    assertThat(sensor.getBlockIndexer(FakeJava.KEY)).isSameAs(sonarEngine);
    assertThat(sensor.getBlockIndexer("PHP")).isSameAs(sonarBridgeEngine);
  }

}
