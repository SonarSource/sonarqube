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
package org.sonar.application;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class JettyEmbedderTest {

  @Test
  public void shouldConfigureProgrammatically() throws Exception {
    JettyEmbedder jetty = new JettyEmbedder("1.2.3.4", 9999);

    assertThat(jetty.getServer().getConnectors()).hasSize(1);
    assertThat(jetty.getServer().getConnectors()[0].getPort()).isEqualTo(9999);
    assertThat(jetty.getServer().getConnectors()[0].getHost()).isEqualTo("1.2.3.4");
  }

  @Test
  public void shouldLoadPluginsClasspath() throws Exception {
    JettyEmbedder jetty = new JettyEmbedder("127.0.0.1", 9999);

    String classpath = jetty.getPluginsClasspath("/org/sonar/application/JettyEmbedderTest/shouldLoadPluginsClasspath");
    classpath = StringUtils.replaceChars(classpath, "\\", "/");

    assertThat(classpath).contains("org/sonar/application/JettyEmbedderTest/shouldLoadPluginsClasspath/plugin1.jar");
    assertThat(classpath).contains("org/sonar/application/JettyEmbedderTest/shouldLoadPluginsClasspath/plugin1.jar");
    assertThat(classpath).contains("org/sonar/application/JettyEmbedderTest/shouldLoadPluginsClasspath/plugin2.jar");

    // important : directories end with /
    assertThat(classpath).contains("org/sonar/application/JettyEmbedderTest/shouldLoadPluginsClasspath/,");
  }
}
