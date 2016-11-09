/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import java.util.List;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.Settings;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.repository.ContextPropertiesCache;

import static java.util.Collections.emptyList;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ContextPropertiesPublisherTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ScannerReportWriter writer = mock(ScannerReportWriter.class);
  private ContextPropertiesCache cache = new ContextPropertiesCache();
  private Settings settings = new MapSettings();
  private ContextPropertiesPublisher underTest = new ContextPropertiesPublisher(cache, settings);

  @Test
  public void publish_writes_properties_to_report() {
    cache.put("foo1", "bar1");
    cache.put("foo2", "bar2");

    underTest.publish(writer);

    List<ScannerReport.ContextProperty> expected = Arrays.asList(
      newContextProperty("foo1", "bar1"),
      newContextProperty("foo2", "bar2"));
    expectWritten(expected);
  }

  @Test
  public void publish_writes_no_properties_to_report() {
    underTest.publish(writer);

    expectWritten(emptyList());
  }

  @Test
  public void publish_settings_prefixed_with_sonar_analysis_for_webhooks() {
    settings.setProperty("foo", "should not be exported");
    settings.setProperty("sonar.analysis.revision", "ab45b3");
    settings.setProperty("sonar.analysis.build.number", "B123");

    underTest.publish(writer);

    List<ScannerReport.ContextProperty> expected = Arrays.asList(
      newContextProperty("sonar.analysis.revision", "ab45b3"),
      newContextProperty("sonar.analysis.build.number", "B123"));
    expectWritten(expected);
  }

  private void expectWritten(List<ScannerReport.ContextProperty> expected) {
    verify(writer).writeContextProperties(argThat(new TypeSafeMatcher<Iterable<ScannerReport.ContextProperty>>() {
      @Override
      protected boolean matchesSafely(Iterable<ScannerReport.ContextProperty> props) {
        List<ScannerReport.ContextProperty> copy = Lists.newArrayList(props);
        copy.removeAll(expected);
        return copy.isEmpty();
      }

      @Override
      public void describeTo(Description description) {
      }
    }));
  }

  private static ScannerReport.ContextProperty newContextProperty(String key, String value) {
    return ScannerReport.ContextProperty.newBuilder()
      .setKey(key)
      .setValue(value)
      .build();
  }
}
