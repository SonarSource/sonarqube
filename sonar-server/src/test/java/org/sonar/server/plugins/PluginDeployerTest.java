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
package org.sonar.server.plugins;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.sonar.api.platform.Server;
import org.sonar.core.plugin.JpaPlugin;
import org.sonar.core.plugin.JpaPluginDao;
import org.sonar.core.plugin.JpaPluginFile;
import org.sonar.jpa.test.AbstractDbUnitTestCase;
import org.sonar.server.platform.DefaultServerFileSystem;
import org.sonar.server.platform.ServerImpl;
import org.sonar.server.platform.ServerStartException;
import org.sonar.test.TestUtils;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class PluginDeployerTest extends AbstractDbUnitTestCase {

  private Server server;
  private JpaPluginDao dao;
  private PluginClassLoaders classloaders;
  private DefaultServerFileSystem fileSystem;
  private File homeDir;
  private File deployDir;
  private PluginDeployer deployer;

  @Rule
  public TestName name = new TestName();

  @Before
  public void start() throws ParseException {
    server = new ServerImpl("1", "2.2", new SimpleDateFormat("yyyy-MM-dd").parse("2010-05-18"));
    dao = new JpaPluginDao(getSessionFactory());
    classloaders = new PluginClassLoaders();
    homeDir = TestUtils.getResource(PluginDeployerTest.class, name.getMethodName());
    deployDir = TestUtils.getTestTempDir(PluginDeployerTest.class, name.getMethodName() + "/deploy");
    fileSystem = new DefaultServerFileSystem(null, homeDir, deployDir);
    deployer = new PluginDeployer(server, fileSystem, dao, classloaders);
  }

  @Test
  public void deployPlugin() throws IOException {
    setupData("shared");
    deployer.start();

    // check that the plugin is registered in database
    List<JpaPlugin> plugins = dao.getPlugins();
    assertThat(plugins.size(), is(1)); // no more checkstyle
    JpaPlugin plugin = plugins.get(0);
    assertThat(plugin.getName(), is("Foo"));
    assertThat(plugin.getFiles().size(), is(1));
    assertThat(plugin.isCore(), is(false));
    JpaPluginFile pluginFile = plugin.getFiles().get(0);
    assertThat(pluginFile.getFilename(), is("foo-plugin.jar"));
    assertThat(pluginFile.getPath(), is("foo/foo-plugin.jar"));

    // check that the file is deployed
    File deployedJar = new File(deployDir, "plugins/foo/foo-plugin.jar");
    assertThat(deployedJar.exists(), is(true));
    assertThat(deployedJar.isFile(), is(true));

    // check that the plugin has its own classloader
    ClassLoader classloader = classloaders.getClassLoader("foo");
    assertNotNull(classloader);
  }

  @Test
  public void deployDeprecatedPlugin() throws IOException, ClassNotFoundException {
    setupData("shared");
    deployer.start();

    // check that the plugin is registered in database
    List<JpaPlugin> plugins = dao.getPlugins();
    assertThat(plugins.size(), is(1)); // no more checkstyle
    JpaPlugin plugin = plugins.get(0);
    assertThat(plugin.getKey(), is("build-breaker"));
    assertThat(plugin.getFiles().size(), is(1));
    assertThat(plugin.isCore(), is(false));
    JpaPluginFile pluginFile = plugin.getFiles().get(0);
    assertThat(pluginFile.getFilename(), is("sonar-build-breaker-plugin-0.1.jar"));
    assertThat(pluginFile.getPath(), is("build-breaker/sonar-build-breaker-plugin-0.1.jar"));

    // check that the file is deployed
    File deployedJar = new File(deployDir, "plugins/build-breaker/sonar-build-breaker-plugin-0.1.jar");
    assertThat(deployedJar.exists(), is(true));
    assertThat(deployedJar.isFile(), is(true));

    // check that the plugin has its own classloader
    ClassLoader classloader = classloaders.getClassLoader("build-breaker");
    assertNotNull(classloader);
    assertNotNull(classloader.loadClass("org.sonar.plugins.buildbreaker.BuildBreakerPlugin"));
  }

  @Test
  public void deployPluginExtensions() throws IOException {
    setupData("shared");
    deployer.start();

    // check that the plugin is registered in database
    List<JpaPlugin> plugins = dao.getPlugins();
    assertThat(plugins.size(), is(1)); // no more checkstyle
    JpaPlugin plugin = plugins.get(0);
    assertThat(plugin.getFiles().size(), is(2));
    JpaPluginFile pluginFile = plugin.getFiles().get(1);
    assertThat(pluginFile.getFilename(), is("foo-extension.txt"));
    assertThat(pluginFile.getPath(), is("foo/foo-extension.txt"));

    // check that the extension file is deployed
    File deployedJar = new File(deployDir, "plugins/foo/foo-extension.txt");
    assertThat(deployedJar.exists(), is(true));
    assertThat(deployedJar.isFile(), is(true));

    // check that the extension is in the classloader
    ClassLoader classloader = classloaders.getClassLoader("foo");
    File extensionFile = FileUtils.toFile(classloader.getResource("foo-extension.txt"));
    assertThat(extensionFile.exists(), is(true));
  }

  @Test
  public void ignoreJarsWhichAreNotPlugins() throws IOException {
    setupData("shared");
    deployer.start();

    // check that the plugin is registered in database
    List<JpaPlugin> plugins = dao.getPlugins();
    assertThat(plugins.size(), is(0));
  }

  @Test(expected = ServerStartException.class)
  public void failIfTwoPluginsWithSameKey() throws IOException {
    setupData("shared");
    deployer.start();
  }

  @Test(expected = ServerStartException.class)
  public void failIfTwoDeprecatedPluginsWithSameKey() throws IOException {
    setupData("shared");
    deployer.start();
  }

}
