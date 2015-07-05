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

package org.sonar.server.source;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.db.measure.MeasureDao;
import org.sonar.server.source.index.SourceLineDoc;
import org.sonar.server.source.index.SourceLineIndex;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SourceServiceTest {

  static final String PROJECT_KEY = "org.sonar.sample";
  static final String COMPONENT_UUID = "abc123";

  @Mock
  HtmlSourceDecorator sourceDecorator;

  @Mock
  MeasureDao measureDao;

  @Mock
  SourceLineIndex sourceLineIndex;

  SourceService service;

  @Before
  public void setUp() {
    service = new SourceService(sourceDecorator, sourceLineIndex);
  }

  @Test
  public void get_html_lines() {
    when(sourceLineIndex.getLines(COMPONENT_UUID, 1, Integer.MAX_VALUE)).thenReturn(
      Arrays.asList(new SourceLineDoc().setSource("source").setHighlighting("highlight").setSymbols("symbols")));

    service.getLinesAsHtml(COMPONENT_UUID, null, null);

    verify(sourceDecorator).getDecoratedSourceAsHtml("source", "highlight", "symbols");
  }

  @Test
  public void get_block_of_lines() {

    when(sourceLineIndex.getLines(COMPONENT_UUID, 1, Integer.MAX_VALUE)).thenReturn(
      Arrays.asList(new SourceLineDoc().setSource("source").setHighlighting("highlight").setSymbols("symbols"),
        new SourceLineDoc().setSource("source2").setHighlighting("highlight2").setSymbols("symbols2")));

    service.getLinesAsHtml(COMPONENT_UUID, null, null);

    verify(sourceDecorator).getDecoratedSourceAsHtml("source", "highlight", "symbols");
    verify(sourceDecorator).getDecoratedSourceAsHtml("source2", "highlight2", "symbols2");
  }

  @Test
  public void getLinesAsTxt() {
    when(sourceLineIndex.getLines(COMPONENT_UUID, 1, Integer.MAX_VALUE)).thenReturn(
      Arrays.asList(
        new SourceLineDoc().setSource("line1"),
        new SourceLineDoc().setSource("line2")));

    List<String> result = service.getLinesAsTxt(COMPONENT_UUID, null, null);
    assertThat(result).contains("line1", "line2");
  }

}
