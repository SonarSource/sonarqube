/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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

import java.util.List;
import org.junit.Test;
import org.sonar.api.Plugin;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.internal.PluginContextImpl;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.Version;
import org.sonar.xoo.global.GlobalProjectSensor;
import org.sonar.xoo.lang.MeasureSensor;
import org.sonar.xoo.scm.XooIgnoreCommand;

import static org.assertj.core.api.Assertions.assertThat;

public class XooPluginTest {

  @Test
  public void provide_extensions_for_sonar_lint() {
    SonarRuntime runtime = SonarRuntimeImpl.forSonarLint(Version.parse("5.4"));
    Plugin.Context context = new PluginContextImpl.Builder().setSonarRuntime(runtime).build();
    new XooPlugin().define(context);

    assertThat(getExtensions(context))
      .isNotEmpty()
      .doesNotContain(MeasureSensor.class);
  }

  @Test
  public void provide_extensions_for_sonar_qube() {
    SonarRuntime runtime = SonarRuntimeImpl.forSonarQube(Version.parse("8.4"), SonarQubeSide.SCANNER, SonarEdition.COMMUNITY);
    Plugin.Context context = new PluginContextImpl.Builder().setSonarRuntime(runtime).build();
    new XooPlugin().define(context);

    assertThat(getExtensions(context))
      .isNotEmpty()
      .contains(MeasureSensor.class)
      .contains(GlobalProjectSensor.class)
      .contains(XooIgnoreCommand.class);
  }

  @SuppressWarnings("unchecked")
  private static List<Object> getExtensions(Plugin.Context context) {
    return (List<Object>) context.getExtensions();
  }
}
