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
package org.sonar.core.plugins;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class RemotePluginTest {
  @Test
  public void shouldEqual() {
    RemotePlugin clirr1 = new RemotePlugin("clirr", false);
    RemotePlugin clirr2 = new RemotePlugin("clirr", false);
    RemotePlugin checkstyle = new RemotePlugin("checkstyle", true);
    assertThat(clirr1.equals(clirr2), is(true));
    assertThat(clirr1.equals(clirr1), is(true));
    assertThat(clirr1.equals(checkstyle), is(false));
  }

  @Test
  public void shouldMarshal() {
    RemotePlugin clirr = new RemotePlugin("clirr", false).setFile("clirr-1.1.jar", "fakemd5");
    String text = clirr.marshal();
    assertThat(text, is("clirr,false,clirr-1.1.jar|fakemd5"));
  }

  @Test
  public void shouldUnmarshal() {
    RemotePlugin clirr = RemotePlugin.unmarshal("clirr,false,clirr-1.1.jar|fakemd5");
    assertThat(clirr.getKey(), is("clirr"));
    assertThat(clirr.isCore(), is(false));
    assertThat(clirr.file().getFilename(), is("clirr-1.1.jar"));
    assertThat(clirr.file().getHash(), is("fakemd5"));
  }
}
