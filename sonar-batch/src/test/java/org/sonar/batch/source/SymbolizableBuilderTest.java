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
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.component.Perspective;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.source.Symbolizable;
import org.sonar.batch.index.BatchComponent;
import org.sonar.batch.sensor.DefaultSensorStorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class SymbolizableBuilderTest {

  @Test
  public void should_load_perspective() {
    Resource file = File.create("foo.c").setEffectiveKey("myproject:path/to/foo.c");
    BatchComponent component = new BatchComponent(1, file, null).setInputComponent(new DefaultInputFile("foo", "foo.c"));

    SymbolizableBuilder perspectiveBuilder = new SymbolizableBuilder(mock(DefaultSensorStorage.class));
    Perspective perspective = perspectiveBuilder.loadPerspective(Symbolizable.class, component);

    assertThat(perspective).isInstanceOf(Symbolizable.class);
  }

  @Test
  public void project_should_not_be_highlightable() {
    BatchComponent component = new BatchComponent(1, new Project("struts").setEffectiveKey("org.struts"), null).setInputComponent(new DefaultInputModule("struts"));

    SymbolizableBuilder builder = new SymbolizableBuilder(mock(DefaultSensorStorage.class));
    Perspective perspective = builder.loadPerspective(Symbolizable.class, component);

    assertThat(perspective).isNull();
  }
}
