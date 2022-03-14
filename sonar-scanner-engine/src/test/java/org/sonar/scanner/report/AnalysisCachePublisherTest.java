/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.scanner.cache.AnalysisCacheEnabled;
import org.sonar.scanner.cache.ScannerWriteCache;
import org.sonar.scanner.protocol.output.ScannerReportWriter;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class AnalysisCachePublisherTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private final ScannerWriteCache writeCache = mock(ScannerWriteCache.class);
  private final AnalysisCacheEnabled analysisCacheEnabled = mock(AnalysisCacheEnabled.class);
  private final AnalysisCachePublisher publisher = new AnalysisCachePublisher(analysisCacheEnabled, writeCache);

  private ScannerReportWriter scannerReportWriter;

  @Before
  public void before() throws IOException {
    scannerReportWriter = new ScannerReportWriter(temp.newFolder());
  }

  @Test
  public void publish_does_nothing_if_cache_not_enabled() {
    when(analysisCacheEnabled.isEnabled()).thenReturn(false);
    publisher.publish(scannerReportWriter);
    verifyNoInteractions(writeCache);
    assertThat(scannerReportWriter.getFileStructure().root()).isEmptyDirectory();
  }

  @Test
  public void publish_cache() {
    when(writeCache.getCache()).thenReturn(Map.of("key1", "value1".getBytes(StandardCharsets.UTF_8)));
    when(analysisCacheEnabled.isEnabled()).thenReturn(true);
    publisher.publish(scannerReportWriter);
    verify(writeCache, times(2)).getCache();
    assertThat(scannerReportWriter.getFileStructure().analysisCache()).exists();
  }

  @Test
  public void publish_empty_cache() {
    when(writeCache.getCache()).thenReturn(emptyMap());
    when(analysisCacheEnabled.isEnabled()).thenReturn(true);
    publisher.publish(scannerReportWriter);
    verify(writeCache).getCache();
    assertThat(scannerReportWriter.getFileStructure().analysisCache()).doesNotExist();
  }
}
