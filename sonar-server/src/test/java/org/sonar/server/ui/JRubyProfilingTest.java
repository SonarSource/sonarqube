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

package org.sonar.server.ui;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.core.profiling.Profiling;

import static org.fest.assertions.Assertions.assertThat;

public class JRubyProfilingTest {

  private JRubyProfiling profilingFacade;

  @Before
  public void initialize() {
    profilingFacade = new JRubyProfiling(new Profiling(new Settings()));
  }

  @Test
  public void should_provide_stop_watch() {
    String domain = "domain";
    assertThat(profilingFacade.start(domain, "FULL")).isNotNull();
  }

  @Test
  public void should_safely_ignore_bad_level_parameter() {
    String domain = "domain";
    assertThat(profilingFacade.start(domain, "POLOP")).isNotNull();
  }
}
