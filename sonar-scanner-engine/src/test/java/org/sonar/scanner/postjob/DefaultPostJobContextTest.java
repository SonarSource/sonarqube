/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.scanner.postjob;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class DefaultPostJobContextTest {

  private DefaultPostJobContext context;
  private MapSettings settings;
  private AnalysisMode analysisMode;

  @Before
  public void setUp() throws IOException {
    settings = new MapSettings();
    analysisMode = mock(AnalysisMode.class);
    context = new DefaultPostJobContext(settings.asConfig(), settings, analysisMode);
  }

  @Test
  public void testSettings() {
    assertThat(context.settings()).isSameAs(settings);
  }

  @Test(expected=UnsupportedOperationException.class)
  public void testIssues() {
    context.issues();
  }

  @Test(expected=UnsupportedOperationException.class)
  public void testResolvedIssues() {
    context.resolvedIssues();
  }
}
