/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.scanner.source;

import org.junit.Test;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.component.Perspective;
import org.sonar.api.source.Symbolizable;
import org.sonar.scanner.sensor.DefaultSensorStorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class SymbolizableBuilderTest {

  @Test
  public void should_load_perspective() {
    SymbolizableBuilder perspectiveBuilder = new SymbolizableBuilder(mock(DefaultSensorStorage.class), mock(AnalysisMode.class));
    Perspective perspective = perspectiveBuilder.loadPerspective(Symbolizable.class, new DefaultInputFile("foo", "foo.c"));

    assertThat(perspective).isInstanceOf(Symbolizable.class);
  }

  @Test
  public void project_should_not_be_highlightable() {
    SymbolizableBuilder builder = new SymbolizableBuilder(mock(DefaultSensorStorage.class), mock(AnalysisMode.class));
    Perspective perspective = builder.loadPerspective(Symbolizable.class, new DefaultInputModule("struts"));

    assertThat(perspective).isNull();
  }
}
