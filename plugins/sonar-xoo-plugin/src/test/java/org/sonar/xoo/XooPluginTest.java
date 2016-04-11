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
package org.sonar.xoo;

import org.junit.Test;
import org.sonar.api.Plugin;
import org.sonar.api.utils.Version;
import org.sonar.xoo.lang.CpdTokenizerSensor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.SonarQubeVersion.V5_5;

public class XooPluginTest {

  @Test
  public void provide_extensions_for_5_5() {
    Plugin.Context context = new Plugin.Context(V5_5);
    new XooPlugin().define(context);
    assertThat(context.getExtensions()).hasSize(39).contains(CpdTokenizerSensor.class);

    context = new Plugin.Context(Version.parse("5.4"));
    new XooPlugin().define(context);
    assertThat(context.getExtensions()).hasSize(38).doesNotContain(CpdTokenizerSensor.class);
  }
}
