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
package org.sonar.xoo.rule.telemetry;

import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.rule.RuleKey;
import org.sonar.xoo.Xoo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class OneIssuePerUninitializedVariableForTelemetrySensorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private final OneIssuePerUninitializedVariableForTelemetrySensor sensor = new OneIssuePerUninitializedVariableForTelemetrySensor(mock(SensorMetrics.class));

  @Test
  public void testDescriptor() {
    DefaultSensorDescriptor descriptor = new DefaultSensorDescriptor();
    sensor.describe(descriptor);
    Configuration configWithProperty = new MapSettings().setProperty(OneIssuePerUninitializedVariableForTelemetrySensor.ACTIVATE, "true").asConfig();
    Configuration configWithoutProperty = new MapSettings().asConfig();

    assertThat(descriptor.languages()).containsOnly(Xoo.KEY);
    assertThat(descriptor.name()).isEqualTo(OneIssuePerUninitializedVariableForTelemetrySensor.NAME);
    assertThat(descriptor.configurationPredicate().test(configWithoutProperty)).isFalse();
    assertThat(descriptor.configurationPredicate().test(configWithProperty)).isTrue();
  }

  @Test
  public void testRule() throws IOException {
    String code = """
      package sample;
      public class Sample {
          a;
          b;
          c;
      
          public Sample() {
              a = 4;
          }
      }
      """;
    DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.xoo")
      .setLanguage(Xoo.KEY)
      .setContents(code)
      .setCharset(Charset.defaultCharset())
      .build();

    SensorContextTester context = SensorContextTester.create(temp.newFolder());
    context.fileSystem().add(inputFile);
    sensor.execute(context);

    assertThat(context.allExternalIssues()).hasSize(2);
    for (ExternalIssue issue : context.allExternalIssues()) {
      assertThat(issue.remediationEffort()).isEqualTo(OneIssuePerUninitializedVariableForTelemetrySensor.EFFORT_MINUTES);
      assertThat(issue.engineId()).isEqualTo(OneIssuePerUninitializedVariableForTelemetrySensor.ENGINE_ID);
      assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("external_XooEngine", OneIssuePerUninitializedVariableForTelemetrySensor.RULE_KEY));
    }
  }

}
