/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.source;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.component.Component;
import org.sonar.batch.index.ComponentDataCache;
import org.sonar.core.source.SnapshotDataType;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class DefaultHighlightableTest {

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  @Test
  public void should_store_highlighting_rules() throws Exception {
    DefaultHighlightable highlightablePerspective = new DefaultHighlightable(null, null);
    highlightablePerspective.newHighlighting().highlight(0, 10, "k").highlight(20, 30, "cppd");

    assertThat(highlightablePerspective.getHighlightingRules().getSortedRules()).hasSize(2);
  }

  @Test
  public void should_apply_registered_highlighting() throws Exception {
    Component component = mock(Component.class);
    when(component.key()).thenReturn("myComponent");

    ComponentDataCache cache = mock(ComponentDataCache.class);

    DefaultHighlightable highlightable = new DefaultHighlightable(component, cache);
    highlightable.newHighlighting()
      .highlight(0, 10, "k")
      .highlight(20, 30, "cppd")
      .done();

    verify(cache).setStringData("myComponent", SnapshotDataType.SYNTAX_HIGHLIGHTING.getValue(), "0,10,k;20,30,cppd;");
  }
}
