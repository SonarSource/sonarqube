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
import org.sonar.api.component.Component;
import org.sonar.api.component.Perspective;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.source.Symbolizable;
import org.sonar.batch.index.BatchResource;
import org.sonar.batch.index.ResourceCache;
import org.sonar.batch.sensor.DefaultSensorStorage;
import org.sonar.core.component.ResourceComponent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SymbolizableBuilderTest {

  @Test
  public void should_load_perspective() throws Exception {
    Resource file = File.create("foo.c").setEffectiveKey("myproject:path/to/foo.c");
    Component component = new ResourceComponent(file);

    ResourceCache resourceCache = mock(ResourceCache.class);
    when(resourceCache.get(file.getEffectiveKey())).thenReturn(new BatchResource(1, file, null).setInputPath(new DefaultInputFile("myproject", "path/to/foo.c")));

    SymbolizableBuilder perspectiveBuilder = new SymbolizableBuilder(resourceCache, mock(DefaultSensorStorage.class));
    Perspective perspective = perspectiveBuilder.loadPerspective(Symbolizable.class, component);

    assertThat(perspective).isInstanceOf(Symbolizable.class);
    assertThat(perspective.component().key()).isEqualTo(component.key());
  }

  @Test
  public void project_should_not_be_highlightable() {
    Component component = new ResourceComponent(new Project("struts").setEffectiveKey("org.struts"));

    SymbolizableBuilder builder = new SymbolizableBuilder(mock(ResourceCache.class), mock(DefaultSensorStorage.class));
    Perspective perspective = builder.loadPerspective(Symbolizable.class, component);

    assertThat(perspective).isNull();
  }
}
