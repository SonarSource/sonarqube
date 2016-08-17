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
import java.util.Map;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.repository.ContextPropertiesCache;

import static com.google.common.collect.FluentIterable.from;

public class ContextPropertiesPublisher implements ReportPublisherStep {
  private final ContextPropertiesCache cache;

  public ContextPropertiesPublisher(ContextPropertiesCache cache) {
    this.cache = cache;
  }

  @Override
  public void publish(ScannerReportWriter writer) {
    Iterable<ScannerReport.ContextProperty> it = from(cache.getAll().entrySet()).transform(new MapEntryToContextPropertyFunction());
    writer.writeContextProperties(it);
  }

  private static final class MapEntryToContextPropertyFunction implements Function<Map.Entry<String, String>, ScannerReport.ContextProperty> {
    private final ScannerReport.ContextProperty.Builder builder = ScannerReport.ContextProperty.newBuilder();

    @Override
    public ScannerReport.ContextProperty apply(Map.Entry<String, String> input) {
      return builder.clear().setKey(input.getKey()).setValue(input.getValue()).build();
    }
  }
}
