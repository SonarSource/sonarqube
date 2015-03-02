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

package org.sonar.server.computation;

import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.core.component.ComponentDto;

import static org.mockito.Mockito.mock;

public class ComputationContextTest {

  ComputationContext sut = new ComputationContext(mock(BatchReportReader.class), mock(ComponentDto.class));

  @Test(expected = IllegalStateException.class)
  public void setProjectSettings() throws Exception {
    sut.setProjectSettings(mock(Settings.class));
    sut.setProjectSettings(mock(Settings.class));
  }
}