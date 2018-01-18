/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.source;

import com.google.common.base.Splitter;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.hash.SourceLinesHashesComputer;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.source.FileSourceDao;
import org.sonar.db.source.FileSourceDto;
import org.sonar.server.computation.task.projectanalysis.component.Component;

import static com.google.common.base.Joiner.on;
import static java.lang.String.valueOf;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.server.computation.task.projectanalysis.component.ReportComponent.builder;

public class SourceLinesDiffImplTest {

  @Rule
  public SourceLinesRepositoryRule sourceLinesRepository = new SourceLinesRepositoryRule();

  private DbClient dbClient = mock(DbClient.class);
  private DbSession dbSession = mock(DbSession.class);
  private ComponentDao componentDao = mock(ComponentDao.class);
  private FileSourceDao fileSourceDao = mock(FileSourceDao.class);

  private static final Splitter END_OF_LINE_SPLITTER = Splitter.on('\n');

  private SourceLinesDiffImpl underTest = new SourceLinesDiffImpl(dbClient, fileSourceDao, sourceLinesRepository);

  private static final int FILE_REF = 1;
  private static final String FILE_KEY = valueOf(FILE_REF);

  private static final String[] CONTENT = {
    "package org.sonar.server.computation.task.projectanalysis.source_diff;",
    "",
    "public class Foo {",
    "  public String bar() {",
    "    return \"Doh!\";",
    "  }",
    "}"
  };

  @Before
  public void setUp() throws Exception {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.componentDao()).thenReturn(componentDao);
    when(dbClient.fileSourceDao()).thenReturn(fileSourceDao);
  }

  @Test
  public void should_find_no_diff_when_report_and_db_content_are_identical() {

    mockContentOfFileInDb("" + FILE_KEY, CONTENT);
    setFileContentInReport(FILE_REF, CONTENT);

    Component component = fileComponent(FILE_REF);
    assertThat(underTest.getMatchingLines(component)).containsExactly(1, 2, 3, 4, 5, 6, 7);

  }

  private void mockContentOfFileInDb(String key, @Nullable String[] content) {
    FileSourceDto dto = new FileSourceDto();
    if (content != null) {
      SourceLinesHashesComputer linesHashesComputer = new SourceLinesHashesComputer();
      stream(content).forEach(linesHashesComputer::addLine);
      dto.setLineHashes(on('\n').join(linesHashesComputer.getLineHashes()));
    }

    when(fileSourceDao.selectLineHashes(dbSession, componentUuidOf(key)))
      .thenReturn(END_OF_LINE_SPLITTER.splitToList(dto.getLineHashes()));
  }

  private static String componentUuidOf(String key) {
    return "uuid_" + key;
  }

  private static Component fileComponent(int ref) {
    return builder(FILE, ref)
      .setPath("report_path" + ref)
      .setUuid(componentUuidOf("" + ref))
      .build();
  }

  private void setFileContentInReport(int ref, String[] content) {
    sourceLinesRepository.addLines(ref, content);
  }
}
