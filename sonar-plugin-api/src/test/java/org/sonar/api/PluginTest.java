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
package org.sonar.api;

import java.util.Arrays;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.PluginContextImpl;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.Version;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginTest {

  private static final Version VERSION_5_6 = Version.create(5, 6);

  @Test
  public void test_context() {
    SonarRuntime runtime = SonarRuntimeImpl.forSonarQube(VERSION_5_6, SonarQubeSide.SERVER, SonarEdition.COMMUNITY);
    MapSettings settings = new MapSettings().setProperty("foo", "bar");
    Plugin.Context context = new PluginContextImpl.Builder()
      .setSonarRuntime(runtime)
      .setBootConfiguration(settings.asConfig())
      .build();

    assertThat(context.getSonarQubeVersion()).isEqualTo(VERSION_5_6);
    assertThat(context.getExtensions()).isEmpty();

    context.addExtension("foo");
    assertThat(context.getExtensions()).containsOnly("foo");

    context.addExtensions(Arrays.asList("bar", "baz"));
    assertThat(context.getExtensions()).containsOnly("foo", "bar", "baz");

    context.addExtensions("one", "two", "three", "four");
    assertThat(context.getExtensions()).containsOnly("foo", "bar", "baz", "one", "two", "three", "four");

    assertThat(context.getBootConfiguration().get("foo")).hasValue("bar");
  }
}
