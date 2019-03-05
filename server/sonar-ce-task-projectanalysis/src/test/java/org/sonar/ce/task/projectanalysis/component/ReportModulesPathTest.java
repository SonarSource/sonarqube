/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.component;

import java.util.Map;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.scanner.protocol.output.ScannerReport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class ReportModulesPathTest {
  private BatchReportReader reader = mock(BatchReportReader.class);
  private ReportModulesPath reportModulesPath = new ReportModulesPath(reader);

  @Test
  public void should_cache_report_data() {
    when(reader.readMetadata()).thenReturn(ScannerReport.Metadata.newBuilder()
      .putModulesProjectRelativePathByKey("module1", "path1")
      .setRootComponentRef(1)
      .build());
    Map<String, String> pathByModuleKey = reportModulesPath.get();
    assertThat(pathByModuleKey).containsExactly(entry("module1", "path1"));
    pathByModuleKey = reportModulesPath.get();
    assertThat(pathByModuleKey).containsExactly(entry("module1", "path1"));

    verify(reader, times(1)).readMetadata();
    verifyNoMoreInteractions(reader);
  }
}
