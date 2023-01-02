/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.report;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.sonar.scanner.ci.CiConfiguration;
import org.sonar.scanner.config.DefaultConfiguration;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.repository.ContextPropertiesCache;
import org.sonar.scanner.scm.ScmConfiguration;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ContextPropertiesPublisherTest {
  private final ScannerReportWriter writer = mock(ScannerReportWriter.class);
  private final ContextPropertiesCache cache = new ContextPropertiesCache();
  private final DefaultConfiguration config = mock(DefaultConfiguration.class);
  private final Map<String, String> props = new HashMap<>();
  private final ScmConfiguration scmConfiguration = mock(ScmConfiguration.class);
  private final CiConfiguration ciConfiguration = mock(CiConfiguration.class);
  private final ContextPropertiesPublisher underTest = new ContextPropertiesPublisher(cache, config, scmConfiguration, ciConfiguration);

  @Before
  public void prepareMock() {
    when(config.getProperties()).thenReturn(props);
    when(ciConfiguration.getCiName()).thenReturn("undetected");
  }

  @Test
  public void publish_writes_properties_to_report() {
    cache.put("foo1", "bar1");
    cache.put("foo2", "bar2");

    underTest.publish(writer);

    List<ScannerReport.ContextProperty> expected = Arrays.asList(
      newContextProperty("foo1", "bar1"),
      newContextProperty("foo2", "bar2"),
      newContextProperty("sonar.analysis.detectedscm", "undetected"),
      newContextProperty("sonar.analysis.detectedci", "undetected"));
    expectWritten(expected);
  }

  @Test
  public void publish_settings_prefixed_with_sonar_analysis_for_webhooks() {
    props.put("foo", "should not be exported");
    props.put("sonar.analysis.revision", "ab45b3");
    props.put("sonar.analysis.build.number", "B123");

    underTest.publish(writer);

    List<ScannerReport.ContextProperty> expected = Arrays.asList(
      newContextProperty("sonar.analysis.revision", "ab45b3"),
      newContextProperty("sonar.analysis.build.number", "B123"),
      newContextProperty("sonar.analysis.detectedscm", "undetected"),
      newContextProperty("sonar.analysis.detectedci", "undetected"));
    expectWritten(expected);
  }

  private void expectWritten(List<ScannerReport.ContextProperty> expected) {
    verify(writer).writeContextProperties(argThat(props -> {
      List<ScannerReport.ContextProperty> copy = Lists.newArrayList(props);
      copy.removeAll(expected);
      return copy.isEmpty();
    }));
  }

  private static ScannerReport.ContextProperty newContextProperty(String key, String value) {
    return ScannerReport.ContextProperty.newBuilder()
      .setKey(key)
      .setValue(value)
      .build();
  }
}
