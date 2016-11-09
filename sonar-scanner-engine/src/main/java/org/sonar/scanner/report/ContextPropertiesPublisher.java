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
import java.util.Map;
import javax.annotation.Nonnull;
import org.sonar.api.config.Settings;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.repository.ContextPropertiesCache;

import static com.google.common.collect.FluentIterable.from;
import static org.sonar.core.config.WebhookProperties.ANALYSIS_PROPERTY_PREFIX;

public class ContextPropertiesPublisher implements ReportPublisherStep {

  private final ContextPropertiesCache cache;
  private final Settings settings;

  public ContextPropertiesPublisher(ContextPropertiesCache cache, Settings settings) {
    this.cache = cache;
    this.settings = settings;
  }

  @Override
  public void publish(ScannerReportWriter writer) {
    MapEntryToContextPropertyFunction transformer = new MapEntryToContextPropertyFunction();

    // properties defined programmatically by plugins
    Iterable<ScannerReport.ContextProperty> fromCache = from(cache.getAll().entrySet())
      .transform(transformer);

    // properties that are automatically included to report so that
    // they can be included to webhook payloads
    Iterable<ScannerReport.ContextProperty> fromSettings = from(settings.getProperties().entrySet())
      .filter(e -> e.getKey().startsWith(ANALYSIS_PROPERTY_PREFIX))
      .transform(transformer);

    writer.writeContextProperties(Iterables.concat(fromCache, fromSettings));
  }

  private static final class MapEntryToContextPropertyFunction implements Function<Map.Entry<String, String>, ScannerReport.ContextProperty> {
    private final ScannerReport.ContextProperty.Builder builder = ScannerReport.ContextProperty.newBuilder();

    @Override
    public ScannerReport.ContextProperty apply(@Nonnull  Map.Entry<String, String> input) {
      return builder.clear().setKey(input.getKey()).setValue(input.getValue()).build();
    }
  }
}
