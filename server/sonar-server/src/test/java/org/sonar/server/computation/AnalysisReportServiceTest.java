/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.computation.db.AnalysisReportDao;
import org.sonar.server.db.DbClient;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class AnalysisReportServiceTest {
  private AnalysisReportService sut;
  private AnalysisReportDao dao;

  @Test
  public void call_dao_to_decompress_report() throws Exception {
    DbClient dbClient = mock(DbClient.class);
    AnalysisReportDao dao = mock(AnalysisReportDao.class);
    when(dbClient.analysisReportDao()).thenReturn(dao);
    sut = new AnalysisReportService(dbClient);
    ComputeEngineContext context = new ComputeEngineContext(mock(AnalysisReportDto.class), mock(ComponentDto.class));

    sut.decompress(mock(DbSession.class), context);

    verify(dao).getDecompressedReport(any(DbSession.class), anyLong());
  }

  @Test
  public void clean_null_directory_does_not_throw_any_exception() throws Exception {
    sut = new AnalysisReportService(mock(DbClient.class));

    sut.clean(null);
  }

  @Test
  public void clean_temp_folder() throws Exception {
    sut = new AnalysisReportService(mock(DbClient.class));
    File origin = new File(getClass().getResource("/org/sonar/server/computation/AnalysisReportServiceTest/fake-report-folder").getFile());
    File destination = new File("target/tmp/report-folder-to-delete");
    FileUtils.copyDirectory(origin, destination);
    assertThat(destination.exists()).isTrue();

    sut.clean(destination);

    assertThat(destination.exists()).isFalse();
  }
}
