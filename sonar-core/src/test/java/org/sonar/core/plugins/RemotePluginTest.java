/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.plugins;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;

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
    RemotePlugin clirr = new RemotePlugin("clirr", false).addFilename("clirr-1.1.jar");
    String text = clirr.marshal();
    assertThat(text, is("clirr,false,clirr-1.1.jar"));
  }

  @Test
  public void shouldMarshalDeprecatedExtensions() {
    RemotePlugin checkstyle = new RemotePlugin("checkstyle", true);
    checkstyle.addFilename("checkstyle-2.8.jar");
    checkstyle.addFilename("ext.xml");
    checkstyle.addFilename("ext.jar");

    String text = checkstyle.marshal();
    assertThat(text, is("checkstyle,true,checkstyle-2.8.jar,ext.xml,ext.jar"));
  }

  @Test
  public void shouldUnmarshal() {
    RemotePlugin clirr = RemotePlugin.unmarshal("clirr,false,clirr-1.1.jar");
    assertThat(clirr.getKey(), is("clirr"));
    assertThat(clirr.isCore(), is(false));
    assertThat(clirr.getFilenames().size(), is(1));
    assertThat(clirr.getFilenames().get(0), is("clirr-1.1.jar"));

  }

  @Test
  public void shouldUnmarshalDeprecatedExtensions() {
    RemotePlugin checkstyle = RemotePlugin.unmarshal("checkstyle,true,checkstyle-2.8.jar,ext.xml,ext.jar");
    assertThat(checkstyle.getKey(), is("checkstyle"));
    assertThat(checkstyle.isCore(), is(true));
    assertThat(checkstyle.getFilenames().size(), is(3));
    assertThat(checkstyle.getFilenames(), hasItems("checkstyle-2.8.jar", "ext.xml", "ext.jar"));
  }
}
