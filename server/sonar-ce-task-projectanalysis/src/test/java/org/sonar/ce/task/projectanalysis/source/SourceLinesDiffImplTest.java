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
package org.sonar.ce.task.projectanalysis.source;

import java.util.Arrays;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.source.FileSourceDao;

import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;

public class SourceLinesDiffImplTest {

  private DbClient dbClient = mock(DbClient.class);
  private DbSession dbSession = mock(DbSession.class);
  private ComponentDao componentDao = mock(ComponentDao.class);
  private FileSourceDao fileSourceDao = mock(FileSourceDao.class);
  private SourceLinesHashRepository sourceLinesHash = mock(SourceLinesHashRepository.class);

  private SourceLinesDiffImpl underTest = new SourceLinesDiffImpl(dbClient, fileSourceDao, sourceLinesHash);

  private static final int FILE_REF = 1;
  private static final String FILE_KEY = valueOf(FILE_REF);

  private static final String[] CONTENT = {
    "package org.sonar.ce.task.projectanalysis.source_diff;",
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
    Component component = fileComponent(FILE_REF);

    mockLineHashesInDb("" + FILE_KEY, CONTENT);
    setLineHashesInReport(component, CONTENT);

    assertThat(underTest.computeMatchingLines(component)).containsExactly(1, 2, 3, 4, 5, 6, 7);

  }

  private void mockLineHashesInDb(String key, @Nullable String[] lineHashes) {
    when(fileSourceDao.selectLineHashes(dbSession, componentUuidOf(key)))
      .thenReturn(Arrays.asList(lineHashes));
  }

  private static String componentUuidOf(String key) {
    return "uuid_" + key;
  }

  private static Component fileComponent(int ref) {
    return builder(FILE, ref)
      .setName("report_path" + ref)
      .setUuid(componentUuidOf("" + ref))
      .build();
  }

  private void setLineHashesInReport(Component component, String[] content) {
    when(sourceLinesHash.getLineHashesMatchingDBVersion(component)).thenReturn(Arrays.asList(content));
  }
}
