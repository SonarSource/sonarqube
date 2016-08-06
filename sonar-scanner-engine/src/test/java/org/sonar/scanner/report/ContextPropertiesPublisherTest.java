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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.Nonnull;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.scanner.repository.ContextPropertiesCache;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportWriter;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ContextPropertiesPublisherTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  ContextPropertiesCache cache = new ContextPropertiesCache();
  ContextPropertiesPublisher underTest = new ContextPropertiesPublisher(cache);

  @Test
  public void publish_writes_properties_to_report() {
    cache.put("foo1", "bar1");
    cache.put("foo2", "bar2");

    ScannerReportWriter writer = mock(ScannerReportWriter.class);
    underTest.publish(writer);

    verify(writer).writeContextProperties(argThat(new TypeSafeMatcher<Iterable<ScannerReport.ContextProperty>>() {
      @Override
      protected boolean matchesSafely(Iterable<ScannerReport.ContextProperty> props) {
        Map<String, ScannerReport.ContextProperty> map = Maps.uniqueIndex(props, ContextPropertyToKey.INSTANCE);
        return map.size() == 2 &&
          map.get("foo1").getValue().equals("bar1") &&
          map.get("foo2").getValue().equals("bar2");
      }

      @Override
      public void describeTo(Description description) {
      }
    }));
  }

  @Test
  public void publish_writes_no_properties_to_report() {
    ScannerReportWriter writer = mock(ScannerReportWriter.class);
    underTest.publish(writer);

    verify(writer).writeContextProperties(argThat(new TypeSafeMatcher<Iterable<ScannerReport.ContextProperty>>() {
      @Override
      protected boolean matchesSafely(Iterable<ScannerReport.ContextProperty> props) {
        return Iterables.isEmpty(props);
      }

      @Override
      public void describeTo(Description description) {
      }
    }));
  }

  private enum ContextPropertyToKey implements Function<ScannerReport.ContextProperty, String> {
    INSTANCE;
    @Override
    public String apply(@Nonnull ScannerReport.ContextProperty input) {
      return input.getKey();
    }
  }
}
