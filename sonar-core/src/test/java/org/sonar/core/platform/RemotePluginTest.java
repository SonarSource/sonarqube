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
package org.sonar.core.platform;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RemotePluginTest {
  @Test
  public void shouldEqual() {
    RemotePlugin clirr1 = new RemotePlugin("clirr");
    RemotePlugin clirr2 = new RemotePlugin("clirr");
    RemotePlugin checkstyle = new RemotePlugin("checkstyle");
    assertThat(clirr1).isEqualTo(clirr2);
    assertThat(clirr1).isEqualTo(clirr1);
    assertThat(clirr1).isNotEqualTo(checkstyle);
  }

  @Test
  public void shouldMarshalNotSonarLintByDefault() {
    RemotePlugin clirr = new RemotePlugin("clirr").setFile("clirr-1.1.jar", "fakemd5");
    String text = clirr.marshal();
    assertThat(text).isEqualTo("clirr,false,clirr-1.1.jar|fakemd5");
  }

  @Test
  public void shouldMarshalSonarLint() {
    RemotePlugin clirr = new RemotePlugin("clirr").setFile("clirr-1.1.jar", "fakemd5").setSonarLintSupported(true);
    String text = clirr.marshal();
    assertThat(text).isEqualTo("clirr,true,clirr-1.1.jar|fakemd5");
  }

  @Test
  public void shouldUnmarshal() {
    RemotePlugin clirr = RemotePlugin.unmarshal("clirr,true,clirr-1.1.jar|fakemd5");
    assertThat(clirr.getKey()).isEqualTo("clirr");
    assertThat(clirr.isSonarLintSupported()).isTrue();
    assertThat(clirr.file().getFilename()).isEqualTo("clirr-1.1.jar");
    assertThat(clirr.file().getHash()).isEqualTo("fakemd5");
  }
}
