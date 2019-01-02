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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.postjob.internal.DefaultPostJobDescriptor;
import org.sonar.api.config.Settings;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;

public class PostJobOptimizerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private PostJobOptimizer optimizer;
  private MapSettings settings;

  @Before
  public void prepare() {
    settings = new MapSettings();
  }

  @Test
  public void should_run_analyzer_with_no_metadata() {
    DefaultPostJobDescriptor descriptor = new DefaultPostJobDescriptor();

    optimizer = new PostJobOptimizer(settings.asConfig());
    assertThat(optimizer.shouldExecute(descriptor)).isTrue();
  }

  @Test
  public void should_optimize_on_settings() {
    DefaultPostJobDescriptor descriptor = new DefaultPostJobDescriptor()
      .requireProperty("sonar.foo.reportPath");
    optimizer = new PostJobOptimizer(settings.asConfig());
    assertThat(optimizer.shouldExecute(descriptor)).isFalse();

    settings.setProperty("sonar.foo.reportPath", "foo");
    optimizer = new PostJobOptimizer(settings.asConfig());
    assertThat(optimizer.shouldExecute(descriptor)).isTrue();
  }
}
