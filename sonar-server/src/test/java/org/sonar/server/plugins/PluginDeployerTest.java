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
package org.sonar.server.plugins;

import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.core.plugins.PluginFileExtractor;
import org.sonar.server.platform.DefaultServerFileSystem;
import org.sonar.server.platform.ServerStartException;
import org.sonar.test.TestUtils;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PluginDeployerTest {

  private PluginFileExtractor extractor;
  private DefaultServerFileSystem fileSystem;
  private File homeDir;
  private File deployDir;
  private PluginDeployer deployer;

  @Rule
  public TestName name = new TestName();

  @Before
  public void start() throws ParseException {
    homeDir = TestUtils.getResource(PluginDeployerTest.class, name.getMethodName());
    deployDir = TestUtils.getTestTempDir(PluginDeployerTest.class, name.getMethodName() + "/deploy");
    fileSystem = new DefaultServerFileSystem(null, homeDir, deployDir);
    extractor = new PluginFileExtractor();
    deployer = new PluginDeployer(fileSystem, extractor);
  }

  @Test
  public void deployPlugin() throws IOException {
    deployer.start();

    // check that the plugin is registered
    assertThat(deployer.getMetadata().size(), Is.is(1)); // no more checkstyle

    PluginMetadata plugin = deployer.getMetadata("foo");
    assertThat(plugin.getName(), is("Foo"));
    assertThat(plugin.getDeployedFiles().size(), is(1));
    assertThat(plugin.isCore(), is(false));
    assertThat(plugin.isUseChildFirstClassLoader(), is(false));
    
    // check that the file is deployed
    File deployedJar = new File(deployDir, "plugins/foo/foo-plugin.jar");
    assertThat(deployedJar.exists(), is(true));
    assertThat(deployedJar.isFile(), is(true));
  }

  @Test
  public void deployDeprecatedPlugin() throws IOException, ClassNotFoundException {
    deployer.start();

    // check that the plugin is registered
    assertThat(deployer.getMetadata().size(), Is.is(1)); // no more checkstyle

    PluginMetadata plugin = deployer.getMetadata("buildbreaker");
    assertThat(plugin.isCore(), is(false));
    assertThat(plugin.isUseChildFirstClassLoader(), is(false));

    // check that the file is deployed
    File deployedJar = new File(deployDir, "plugins/buildbreaker/sonar-build-breaker-plugin-0.1.jar");
    assertThat(deployedJar.exists(), is(true));
    assertThat(deployedJar.isFile(), is(true));
  }

  @Test
  public void deployPluginExtensions() throws IOException {
    deployer.start();

    // check that the plugin is registered 
    assertThat(deployer.getMetadata().size(), Is.is(1)); // no more checkstyle

    PluginMetadata plugin = deployer.getMetadata("foo");
    assertThat(plugin.getDeployedFiles().size(), is(2));
    File extFile = plugin.getDeployedFiles().get(1);
    assertThat(extFile.getName(), is("foo-extension.txt"));

    // check that the extension file is deployed
    File deployedJar = new File(deployDir, "plugins/foo/foo-extension.txt");
    assertThat(deployedJar.exists(), is(true));
    assertThat(deployedJar.isFile(), is(true));
  }

  @Test
  public void ignoreJarsWhichAreNotPlugins() throws IOException {
    deployer.start();

    assertThat(deployer.getMetadata().size(), Is.is(0));
  }

  @Test(expected = ServerStartException.class)
  public void failIfTwoPluginsWithSameKey() throws IOException {
    deployer.start();
  }

  @Test(expected = ServerStartException.class)
  public void failIfTwoDeprecatedPluginsWithSameKey() throws IOException {
    deployer.start();
  }

}
