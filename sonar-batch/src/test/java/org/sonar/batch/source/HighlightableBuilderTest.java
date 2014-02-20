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

import org.junit.Test;
import org.sonar.api.component.Component;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.source.Highlightable;
import org.sonar.batch.index.ComponentDataCache;
import org.sonar.core.component.ResourceComponent;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class HighlightableBuilderTest {

  ComponentDataCache cache = mock(ComponentDataCache.class);

  @Test
  public void should_load_default_perspective() throws Exception {
    Resource file = new File("foo.c").setEffectiveKey("myproject:path/to/foo.c");
    Component component = new ResourceComponent(file);

    HighlightableBuilder builder = new HighlightableBuilder(cache);
    Highlightable perspective = builder.loadPerspective(Highlightable.class, component);

    assertThat(perspective).isNotNull().isInstanceOf(DefaultHighlightable.class);
    assertThat(perspective.component()).isSameAs(component);
  }

  @Test
  public void project_should_not_be_highlightable() {
    Component component = new ResourceComponent(new Project("struts").setEffectiveKey("org.struts"));

    HighlightableBuilder builder = new HighlightableBuilder(cache);
    Highlightable perspective = builder.loadPerspective(Highlightable.class, component);

    assertThat(perspective).isNull();
  }
}
