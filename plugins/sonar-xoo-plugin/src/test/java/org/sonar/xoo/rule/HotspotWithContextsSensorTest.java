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
package org.sonar.xoo.rule;

import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.xoo.Xoo;
import org.sonar.xoo.rule.hotspot.HotspotWithContextsSensor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HotspotWithContextsSensorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private final ActiveRules activeRules = mock(ActiveRules.class);

  @Before
  public void before() {
    when(activeRules.find(any())).thenReturn(mock(ActiveRule.class));
  }

  @Test
  public void processFile_givenCorrectTagPassed_oneSecurityHotspotWithContextsIsRaised() throws IOException {
    DefaultInputFile inputFile = newTestFile(HotspotWithContextsSensor.TAG + "/n some text /n");

    DefaultFileSystem fs = new DefaultFileSystem(temp.newFolder());
    fs.add(inputFile);

    SensorContextTester sensorContextTester = SensorContextTester.create(temp.newFolder().toPath());
    HotspotWithContextsSensor sensor = new HotspotWithContextsSensor(fs, activeRules);

    sensor.execute(sensorContextTester);

    assertThat(sensorContextTester.allIssues()).hasSize(1);
  }

  @Test
  public void processFile_givenJustHotspotTagPassed_noSecurityHotspotWithContextAreRaised() throws IOException {
    DefaultInputFile inputFile = newTestFile("HOTSPOT/n hotspot hotspot some text /n hotspot /n text");

    DefaultFileSystem fs = new DefaultFileSystem(temp.newFolder());
    fs.add(inputFile);

    SensorContextTester sensorContextTester = SensorContextTester.create(temp.newFolder().toPath());
    HotspotWithContextsSensor sensor = new HotspotWithContextsSensor(fs, activeRules);

    sensor.execute(sensorContextTester);

    assertThat(sensorContextTester.allIssues()).isEmpty();
  }

  private DefaultInputFile newTestFile(String content) {
    return new TestInputFileBuilder("foo", "hotspot.xoo")
      .setLanguage(Xoo.KEY)
      .setContents(content)
      .setCharset(Charset.defaultCharset())
      .build();
  }
}
