/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.server.mavendeployer;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.sonar.server.platform.DefaultServerFileSystem;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MavenRepositoryTest {

  private MavenRepository repository;
  private File deployDir;

  @Before
  public void before() throws IOException {
    DefaultServerFileSystem extensionsFinder = mock(DefaultServerFileSystem.class);
    when(extensionsFinder.getMaven2Plugin()).thenReturn(getFile(("sonar-core-maven-plugin/sonar-core-maven-plugin-1.8-SNAPSHOT.jar")));

    deployDir = new File("./target/test-tmp/MavenRepositoryTest/");
    FileUtils.deleteQuietly(deployDir);

    repository = new MavenRepository(extensionsFinder, "123456789", deployDir);
  }

  private File getFile(String path) {
    URL url = getClass().getResource("/org/sonar/server/mavendeployer/MavenRepositoryTest/extensions/" + path);
    return FileUtils.toFile(url);
  }

  @Test
  public void shouldDeployMojo() throws IOException {
    repository.start();
    assertThat(new File(deployDir, "org/codehaus/sonar/runtime/sonar-core-maven-plugin/123456789/sonar-core-maven-plugin-123456789.pom").exists(), is(true));
    assertThat(new File(deployDir, "org/codehaus/sonar/runtime/sonar-core-maven-plugin/123456789/sonar-core-maven-plugin-123456789.jar").exists(), is(true));
  }
}
