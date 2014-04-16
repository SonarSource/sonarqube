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
package org.sonar.server.startup;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;

public class GenerateBootstrapIndexTest {

  @Rule
  public TemporaryFolder rootDir = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
  }

  @Test
  public void determine_list_of_resources() throws IOException {
    new File(rootDir.getRoot(), "/web/WEB-INF/lib").mkdirs();
    File webInf = new File(rootDir.getRoot(), "/web/WEB-INF");
    File lib = new File(rootDir.getRoot(), "/web/WEB-INF/lib");
    new File(webInf, "directory").mkdir();
    new File(lib, "sonar-core-2.6.jar").createNewFile();
    new File(lib, "treemap.rbr").createNewFile();
    new File(lib, "sonar-core-2.6.jar").createNewFile();

    assertThat(GenerateBootstrapIndex.getLibs(lib)).hasSize(1);
    assertThat(GenerateBootstrapIndex.getLibs(lib).get(0)).isEqualTo("sonar-core-2.6.jar");
  }

  @Test
  public void ignore_some_jars() {
    assertThat(GenerateBootstrapIndex.isIgnored("sonar-batch-2.6-SNAPSHOT.jar")).isFalse();
    assertThat(GenerateBootstrapIndex.isIgnored("mysql-connector-java-5.1.13.jar")).isTrue();
    assertThat(GenerateBootstrapIndex.isIgnored("postgresql-9.0-801.jdbc3.jar")).isTrue();
    assertThat(GenerateBootstrapIndex.isIgnored("jtds-1.2.4.jar")).isTrue();
    assertThat(GenerateBootstrapIndex.isIgnored("jfreechart-1.0.9.jar")).isTrue();
    assertThat(GenerateBootstrapIndex.isIgnored("eastwood-1.1.0.jar")).isTrue();
    assertThat(GenerateBootstrapIndex.isIgnored("jruby-complete-1.5.6.jar")).isTrue();
    assertThat(GenerateBootstrapIndex.isIgnored("jruby-rack-1.0.5.jar")).isTrue();
    assertThat(GenerateBootstrapIndex.isIgnored("elasticsearch-0.90.6.jar")).isTrue();
    assertThat(GenerateBootstrapIndex.isIgnored("lucene-core-4.5.1.jar")).isTrue();
  }

}
