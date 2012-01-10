/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.java.bytecode.loader;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import org.junit.Test;
import org.sonar.java.ast.SquidTestUtils;

public class FileSystemLoaderTest {

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowIllegalArgumentException() throws Exception {
    new FileSystemLoader(null);
  }

  @Test
  public void testFindResource() throws Exception {
    File dir = SquidTestUtils.getFile("/bytecode/bin/");
    FileSystemLoader loader = new FileSystemLoader(dir);

    assertThat(loader.findResource("notfound"), nullValue());

    URL url = loader.findResource("tags/TagName.class");
    assertThat(url, notNullValue());
    assertThat(url.toString(), allOf(startsWith("file:"), endsWith("TagName.class")));

    loader.close();

    try {
      loader.findResource("tags/TagName.class");
      fail();
    } catch (IllegalStateException e) {
      // ok
    }
  }

  @Test
  public void testLoadBytes() throws Exception {
    File dir = SquidTestUtils.getFile("/bytecode/bin/");
    FileSystemLoader loader = new FileSystemLoader(dir);

    assertThat(loader.loadBytes("notfound"), nullValue());

    assertThat(loader.loadBytes("tags/TagName.class"), notNullValue());

    loader.close();

    try {
      loader.loadBytes("tags/TagName.class");
      fail();
    } catch (IllegalStateException e) {
      // ok
    }
  }

  @Test
  public void closeCanBeCalledMultipleTimes() throws Exception {
    File dir = SquidTestUtils.getFile("/bytecode/bin/");
    FileSystemLoader loader = new FileSystemLoader(dir);
    loader.close();
    loader.close();
  }

}
