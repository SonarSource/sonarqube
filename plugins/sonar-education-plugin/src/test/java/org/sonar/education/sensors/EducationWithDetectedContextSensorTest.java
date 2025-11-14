/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.education.sensors;

import java.io.IOException;
import org.junit.Test;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.internal.SensorContextTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.education.sensors.EducationWithDetectedContextSensor.EDUCATION_WITH_DETECTED_CONTEXT_RULE_KEY;

public class EducationWithDetectedContextSensorTest extends EducationSensorTest {

  @Test
  public void processFile_givenCorrectTagPassed_oneSecurityHotspotWithContextsIsRaised() throws IOException {
    DefaultInputFile inputFile = newTestFile(EDUCATION_WITH_DETECTED_CONTEXT_RULE_KEY);

    DefaultFileSystem fs = new DefaultFileSystem(temp.newFolder());
    fs.add(inputFile);

    SensorContextTester sensorContextTester = SensorContextTester.create(temp.newFolder().toPath());
    var sensor = new EducationWithDetectedContextSensor(fs, activeRules);

    sensor.execute(sensorContextTester);

    assertThat(sensorContextTester.allIssues()).hasSize(1);
  }
}
