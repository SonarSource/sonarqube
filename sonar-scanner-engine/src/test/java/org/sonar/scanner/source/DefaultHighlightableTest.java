/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.source;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.highlighting.internal.DefaultHighlighting;
import org.sonar.api.batch.sensor.internal.SensorStorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DefaultHighlightableTest {

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  @Test
  public void should_store_highlighting_rules() {
    SensorStorage sensorStorage = mock(SensorStorage.class);
    DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.php")
      .initMetadata("azerty\nbla bla")
      .build();
    DefaultHighlightable highlightablePerspective = new DefaultHighlightable(inputFile, sensorStorage, mock(AnalysisMode.class));
    highlightablePerspective.newHighlighting().highlight(0, 6, "k").highlight(7, 10, "cppd").done();

    ArgumentCaptor<DefaultHighlighting> argCaptor = ArgumentCaptor.forClass(DefaultHighlighting.class);
    verify(sensorStorage).store(argCaptor.capture());
    assertThat(argCaptor.getValue().getSyntaxHighlightingRuleSet()).hasSize(2);
  }

}
