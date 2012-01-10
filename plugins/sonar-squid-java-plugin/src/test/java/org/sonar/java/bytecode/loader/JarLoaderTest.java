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
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sonar.java.ast.SquidTestUtils;

public class JarLoaderTest {

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowIllegalArgumentException() throws Exception {
    new JarLoader(null);
  }

  @Test
  public void testFindResource() throws Exception {
    File jar = SquidTestUtils.getFile("/bytecode/lib/hello.jar");
    JarLoader loader = new JarLoader(jar);

    assertThat(loader.findResource("notfound"), nullValue());

    URL url = loader.findResource("META-INF/MANIFEST.MF");
    assertThat(url, notNullValue());
    assertThat(url.toString(), allOf(startsWith("jar:"), endsWith("hello.jar!/META-INF/MANIFEST.MF")));

    InputStream is = url.openStream();
    try {
      assertThat(IOUtils.readLines(is), hasItem("Manifest-Version: 1.0"));
    } finally {
      IOUtils.closeQuietly(is);
    }

    loader.close();

    try {
      loader.findResource("META-INF/MANIFEST.MF");
      fail();
    } catch (IllegalStateException e) {
      // ok
    }
  }

  @Test
  public void testLoadBytes() throws Exception {
    File jar = SquidTestUtils.getFile("/bytecode/lib/hello.jar");
    JarLoader loader = new JarLoader(jar);

    assertThat(loader.loadBytes("notfound"), nullValue());

    byte[] bytes = loader.loadBytes("META-INF/MANIFEST.MF");
    assertThat(bytes, notNullValue());
    ByteArrayInputStream is = new ByteArrayInputStream(bytes);
    assertThat(IOUtils.readLines(is), hasItem("Manifest-Version: 1.0"));

    loader.close();

    try {
      loader.loadBytes("META-INF/MANIFEST.MF");
      fail();
    } catch (IllegalStateException e) {
      // ok
    }
  }

  @Test
  public void closeCanBeCalledMultipleTimes() throws Exception {
    File jar = SquidTestUtils.getFile("/bytecode/lib/hello.jar");
    JarLoader loader = new JarLoader(jar);
    loader.close();
    loader.close();
  }

}
