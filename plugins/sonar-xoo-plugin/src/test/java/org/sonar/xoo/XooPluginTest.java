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
package org.sonar.xoo;

import org.junit.Test;
import org.sonar.api.Plugin;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.internal.PluginContextImpl;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.Version;
import org.sonar.xoo.lang.CpdTokenizerSensor;

import static org.assertj.core.api.Assertions.assertThat;

public class XooPluginTest {

  @Test
  public void provide_extensions_for_5_4() {
    SonarRuntime runtime = SonarRuntimeImpl.forSonarLint(Version.parse("5.4"));
    Plugin.Context context = new PluginContextImpl.Builder().setSonarRuntime(runtime).build();
    new XooPlugin().define(context);
    assertThat(context.getExtensions()).hasSize(47).doesNotContain(CpdTokenizerSensor.class);
  }

  @Test
  public void provide_extensions_for_5_5() {
    SonarRuntime runtime = SonarRuntimeImpl.forSonarQube(Version.parse("5.5"), SonarQubeSide.SCANNER);
    Plugin.Context context = new PluginContextImpl.Builder().setSonarRuntime(runtime).build();
    new XooPlugin().define(context);
    assertThat(context.getExtensions()).hasSize(50).contains(CpdTokenizerSensor.class);
  }

  @Test
  public void provide_extensions_for_6_6() {
    SonarRuntime runtime = SonarRuntimeImpl.forSonarQube(Version.parse("6.6"), SonarQubeSide.SCANNER);
    Plugin.Context context = new PluginContextImpl.Builder().setSonarRuntime(runtime).build();
    new XooPlugin().define(context);
    assertThat(context.getExtensions()).hasSize(51).contains(CpdTokenizerSensor.class);
  }
}
