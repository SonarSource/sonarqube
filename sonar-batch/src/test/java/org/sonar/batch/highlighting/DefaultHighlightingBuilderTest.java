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
package org.sonar.batch.highlighting;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.batch.index.ComponentDataCache;
import org.sonar.core.source.SnapshotDataTypes;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DefaultHighlightingBuilderTest {

  @Test
  public void should_apply_registered_highlighting() throws Exception {

    ComponentDataCache cache = mock(ComponentDataCache.class);

    DefaultHighlightingBuilder highlightable = new DefaultHighlightingBuilder("myComponent", cache);
    highlightable
      .highlight(0, 10, TypeOfText.KEYWORD)
      .highlight(20, 30, TypeOfText.CPP_DOC)
      .done();

    ArgumentCaptor<SyntaxHighlightingData> argCaptor = ArgumentCaptor.forClass(SyntaxHighlightingData.class);
    verify(cache).setData(eq("myComponent"), eq(SnapshotDataTypes.SYNTAX_HIGHLIGHTING), argCaptor.capture());
    assertThat(argCaptor.getValue().writeString()).isEqualTo("0,10,k;20,30,cppd;");
  }
}
