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
package org.sonar.batch.source;

import org.junit.Test;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.component.Component;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.source.Highlightable;
import org.sonar.batch.index.BatchResource;
import org.sonar.batch.index.ResourceCache;
import org.sonar.core.component.ResourceComponent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HighlightableBuilderTest {

  @Test
  public void should_load_default_perspective() throws Exception {
    Resource file = File.create("foo.c").setEffectiveKey("myproject:path/to/foo.c");
    Component component = new ResourceComponent(file);

    ResourceCache resourceCache = mock(ResourceCache.class);
    when(resourceCache.get(file.getEffectiveKey())).thenReturn(new BatchResource(1, file, null).setInputPath(new DefaultInputFile("myproject", "path/to/foo.c")));
    HighlightableBuilder builder = new HighlightableBuilder(resourceCache, mock(SensorStorage.class));
    Highlightable perspective = builder.loadPerspective(Highlightable.class, component);

    assertThat(perspective).isNotNull().isInstanceOf(DefaultHighlightable.class);
    assertThat(perspective.component().key()).isEqualTo(component.key());
  }

  @Test
  public void project_should_not_be_highlightable() {
    Component component = new ResourceComponent(new Project("struts").setEffectiveKey("org.struts"));

    HighlightableBuilder builder = new HighlightableBuilder(mock(ResourceCache.class), mock(SensorStorage.class));
    Highlightable perspective = builder.loadPerspective(Highlightable.class, component);

    assertThat(perspective).isNull();
  }
}
