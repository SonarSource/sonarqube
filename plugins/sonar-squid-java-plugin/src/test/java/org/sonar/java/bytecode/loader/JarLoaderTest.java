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
package org.sonar.java.bytecode.loader;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.sonar.java.ast.SquidTestUtils;

public class JarLoaderTest {

  @Test
  public void testFindResource() throws Exception {
    File jar = SquidTestUtils.getFile("/bytecode/lib/hello.jar");
    JarLoader jarLoader = new JarLoader(jar);

    URL url = jarLoader.findResource("META-INF/MANIFEST.MF");
    assertThat(url.toString(), allOf(startsWith("jar:"), endsWith("hello.jar!/META-INF/MANIFEST.MF")));

    InputStream is = url.openStream();
    try {
      assertThat(IOUtils.readLines(is), hasItem("Manifest-Version: 1.0"));
    } finally {
      IOUtils.closeQuietly(is);
    }

    jarLoader.close();
  }

}
