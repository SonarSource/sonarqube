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
package org.sonar.server.source;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.source.FileSourceDto;
import org.sonar.server.source.index.FileSourceTesting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SourceServiceTest {

  public static final String FILE_UUID = "FILE_UUID";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  HtmlSourceDecorator htmlDecorator = mock(HtmlSourceDecorator.class);

  SourceService underTest = new SourceService(dbTester.getDbClient(), htmlDecorator);

  @Before
  public void injectFakeLines() throws IOException {
    FileSourceDto dto = new FileSourceDto();
    dto.setFileUuid(FILE_UUID).setProjectUuid("PROJECT_UUID");
    dto.setSourceData(FileSourceTesting.newFakeData(10).build());
    dbTester.getDbClient().fileSourceDao().insert(dbTester.getSession(), dto);
    dbTester.commit();
  }

  @Test
  public void get_range_of_lines() {
    Optional<Iterable<DbFileSources.Line>> linesOpt = underTest.getLines(dbTester.getSession(), FILE_UUID, 5, 7);
    assertThat(linesOpt.isPresent()).isTrue();
    List<DbFileSources.Line> lines = Lists.newArrayList(linesOpt.get());
    assertThat(lines).hasSize(3);
    assertThat(lines.get(0).getLine()).isEqualTo(5);
    assertThat(lines.get(1).getLine()).isEqualTo(6);
    assertThat(lines.get(2).getLine()).isEqualTo(7);
  }

  @Test
  public void get_range_of_lines_as_raw_text() {
    Optional<Iterable<String>> linesOpt = underTest.getLinesAsRawText(dbTester.getSession(), FILE_UUID, 5, 7);
    assertThat(linesOpt.isPresent()).isTrue();
    List<String> lines = Lists.newArrayList(linesOpt.get());
    assertThat(lines).containsExactly("SOURCE_5", "SOURCE_6", "SOURCE_7");
  }

  @Test
  public void get_range_of_lines_as_html() {
    when(htmlDecorator.getDecoratedSourceAsHtml("SOURCE_5", "HIGHLIGHTING_5", "SYMBOLS_5")).thenReturn("HTML_5");
    when(htmlDecorator.getDecoratedSourceAsHtml("SOURCE_6", "HIGHLIGHTING_6", "SYMBOLS_6")).thenReturn("HTML_6");
    when(htmlDecorator.getDecoratedSourceAsHtml("SOURCE_7", "HIGHLIGHTING_7", "SYMBOLS_7")).thenReturn("HTML_7");

    Optional<Iterable<String>> linesOpt = underTest.getLinesAsHtml(dbTester.getSession(), FILE_UUID, 5, 7);
    assertThat(linesOpt.isPresent()).isTrue();
    List<String> lines = Lists.newArrayList(linesOpt.get());
    assertThat(lines).containsExactly("HTML_5", "HTML_6", "HTML_7");
  }

  @Test
  public void getLines_fails_if_range_starts_at_zero() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Line number must start at 1, got 0");

    underTest.getLines(dbTester.getSession(), FILE_UUID, 0, 2);
  }

  @Test
  public void getLines_fails_if_range_upper_bound_less_than_lower_bound() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Line number must greater than or equal to 5, got 4");

    underTest.getLines(dbTester.getSession(), FILE_UUID, 5, 4);
  }

  @Test
  public void getLines_returns_empty_iterable_if_range_is_out_of_scope() {
    Optional<Iterable<DbFileSources.Line>> lines = underTest.getLines(dbTester.getSession(), FILE_UUID, 500, 510);
    assertThat(lines.isPresent()).isTrue();
    assertThat(lines.get()).isEmpty();
  }

  @Test
  public void getLines_file_does_not_exist() {
    Optional<Iterable<DbFileSources.Line>> lines = underTest.getLines(dbTester.getSession(), "FILE_DOES_NOT_EXIST", 1, 10);
    assertThat(lines.isPresent()).isFalse();
  }

}
