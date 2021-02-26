/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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

import java.util.AbstractMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.sonar.api.batch.scm.ScmProvider;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.scanner.config.DefaultConfiguration;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.repository.ContextPropertiesCache;
import org.sonar.scanner.scm.ScmConfiguration;

import static org.sonar.core.config.CorePropertyDefinitions.SONAR_ANALYSIS_DETECTEDSCM;

public class ContextPropertiesPublisher implements ReportPublisherStep {
  private final ContextPropertiesCache cache;
  private final DefaultConfiguration config;
  private final ScmConfiguration scmConfiguration;

  public ContextPropertiesPublisher(ContextPropertiesCache cache, DefaultConfiguration config, ScmConfiguration scmConfiguration) {
    this.cache = cache;
    this.config = config;
    this.scmConfiguration = scmConfiguration;
  }

  @Override
  public void publish(ScannerReportWriter writer) {
    MapEntryToContextPropertyFunction transformer = new MapEntryToContextPropertyFunction();

    // properties defined programmatically by plugins
    Stream<ScannerReport.ContextProperty> fromCache = Stream.concat(cache.getAll().entrySet().stream(), Stream.of(constructScmInfo())).map(transformer);

    // properties that are automatically included to report so that
    // they can be included to webhook payloads
    Stream<ScannerReport.ContextProperty> fromSettings = config.getProperties().entrySet().stream()
      .filter(e -> e.getKey().startsWith(CorePropertyDefinitions.SONAR_ANALYSIS))
      .map(transformer);

    writer.writeContextProperties(Stream.concat(fromCache, fromSettings).collect(Collectors.toList()));
  }

  private Map.Entry<String, String> constructScmInfo() {
    ScmProvider scmProvider = scmConfiguration.provider();
    if (scmProvider != null) {
      return new AbstractMap.SimpleEntry<>(SONAR_ANALYSIS_DETECTEDSCM, scmProvider.key());
    } else {
      return new AbstractMap.SimpleEntry<>(SONAR_ANALYSIS_DETECTEDSCM, "undetected");
    }
  }

  private static final class MapEntryToContextPropertyFunction implements Function<Map.Entry<String, String>, ScannerReport.ContextProperty> {
    private final ScannerReport.ContextProperty.Builder builder = ScannerReport.ContextProperty.newBuilder();

    @Override
    public ScannerReport.ContextProperty apply(@Nonnull Map.Entry<String, String> input) {
      return builder.clear().setKey(input.getKey()).setValue(input.getValue()).build();
    }
  }
}
