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
package org.sonar.server.app;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class WebDeployContextTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  Tomcat tomcat = mock(Tomcat.class);
  Properties props = new Properties();

  @Test
  public void create_dir_and_configure_tomcat_context() throws Exception {
    File dataDir = temp.newFolder();
    props.setProperty(ProcessProperties.PATH_DATA, dataDir.getAbsolutePath());
    new WebDeployContext().configureTomcat(tomcat, new Props(props));

    File deployDir = new File(dataDir, "web/deploy");
    assertThat(deployDir).isDirectory().exists();
    verify(tomcat).addWebapp("/deploy", deployDir.getAbsolutePath());
  }

  @Test
  public void cleanup_directory_if_already_exists() throws Exception {
    File dataDir = temp.newFolder();
    File deployDir = new File(dataDir, "web/deploy");
    FileUtils.touch(new File(deployDir, "foo.txt"));
    props.setProperty(ProcessProperties.PATH_DATA, dataDir.getAbsolutePath());
    new WebDeployContext().configureTomcat(tomcat, new Props(props));

    assertThat(deployDir).isDirectory().exists();
    assertThat(deployDir.listFiles()).isEmpty();
  }

  @Test
  public void fail_if_directory_can_not_be_initialized() throws Exception {
    File dataDir = temp.newFolder();
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to create or clean-up directory " + dataDir.getAbsolutePath());

    props.setProperty(ProcessProperties.PATH_DATA, dataDir.getAbsolutePath());
    WebDeployContext.Fs fs = mock(WebDeployContext.Fs.class);
    doThrow(new IOException()).when(fs).createOrCleanupDir(any(File.class));

    new WebDeployContext(fs).configureTomcat(tomcat, new Props(props));

  }
}
