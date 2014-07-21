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
package org.sonar.server.tester;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.rules.ExternalResource;
import org.sonar.api.CoreProperties;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.server.platform.Platform;
import org.sonar.server.search.IndexProperties;
import org.sonar.server.ws.WsTester;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Entry point to implement medium tests of server components.
 * <p/>
 * The system properties starting with "mediumTests." override the programmatic settings, for example:
 * <code>-DmediumTests.sonar.log.profilingLevel=FULL</code>
 *
 * @since 4.4
 */
public class ServerTester extends ExternalResource {

  private static final String PROP_PREFIX = "mediumTests.";

  private final Platform platform;
  private final File homeDir;
  private final List components = Lists.newArrayList(BackendCleanup.class, WsTester.class);
  private final Properties initialProps = new Properties();

  public ServerTester() {
    homeDir = createTempDir();
    platform = new Platform();
  }

  /**
   * Called only when JUnit @Rule or @ClassRule is used.
   */
  @Override
  protected void before() {
    start();
  }

  /**
   * This method should not be called by test when ServerTester is annotated with {@link org.junit.Rule}
   */
  public void start() {
    checkNotStarted();

    Properties properties = new Properties();
    properties.putAll(initialProps);
    properties.setProperty(IndexProperties.TYPE, IndexProperties.ES_TYPE.MEMORY.name());
    properties.setProperty("sonar.path.home", homeDir.getAbsolutePath());
    properties.setProperty(DatabaseProperties.PROP_URL, "jdbc:h2:" + homeDir.getAbsolutePath() + "/h2");
    for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
      String key = entry.getKey().toString();
      if (key.startsWith(PROP_PREFIX)) {
        properties.put(StringUtils.substringAfter(key, PROP_PREFIX), entry.getValue());
      }
    }

    platform.init(properties);
    platform.addComponents(components);
    platform.doStart();
    if (!platform.isStarted()) {
      throw new IllegalStateException("Server not started. You should check that db migrations " +
        "are correctly declared, for example in schema-h2.sql or DatabaseVersion");
    }
  }

  private File createTempDir() {
    try {
      // Technique to create a temp directory from a temp file
      File f = File.createTempFile("SonarQube", "");
      f.delete();
      f.mkdir();
      return f;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to create temp dir", e);
    }
  }

  /**
   * Called only when JUnit @Rule or @ClassRule is used.
   */
  @Override
  protected void after() {
    stop();
  }

  /**
   * This method should not be called by test when ServerTester is annotated with {@link org.junit.Rule}
   */
  public void stop() {
    platform.doStop();
    FileUtils.deleteQuietly(homeDir);
  }

  /**
   * Add classes or objects to IoC container, as it could be done by plugins.
   * Must be called before {@link #start()}.
   */
  public ServerTester addComponents(@Nullable Object... components) {
    checkNotStarted();
    if (components != null) {
      this.components.addAll(Arrays.asList(components));
    }
    return this;
  }

  public ServerTester addPluginJar(File jar) {
    Preconditions.checkArgument(jar.exists() && jar.isFile(), "Plugin JAR file does not exist: " + jar.getAbsolutePath());
    try {
      File pluginsDir = new File(homeDir, "extensions/plugins");
      FileUtils.forceMkdir(pluginsDir);
      FileUtils.copyFileToDirectory(jar, pluginsDir);
      return this;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to copy plugin JAR file: " + jar.getAbsolutePath(), e);
    }
  }

  /**
   * Set a property available for startup. Must be called before {@link #start()}.
   */
  public ServerTester setProperty(String key, String value) {
    checkNotStarted();
    initialProps.setProperty(key, value);
    return this;
  }

  /**
   * Truncate all db tables and Elasticsearch indexes. Can be executed only if ServerTester is started.
   */
  public void clearDbAndIndexes() {
    checkStarted();
    get(BackendCleanup.class).clearAll();
  }

  public void clearIndexes() {
    checkStarted();
    get(BackendCleanup.class).clearIndexes();
  }

  /**
   * Get a component from the platform
   */
  public <C> C get(Class<C> component) {
    checkStarted();
    return platform.getContainer().getComponentByType(component);
  }

  public WsTester wsTester() {
    return get(WsTester.class);
  }

  private void checkStarted() {
    if (!platform.isStarted()) {
      throw new IllegalStateException("Not started");
    }
  }

  private void checkNotStarted() {
    if (platform.isStarted()) {
      throw new IllegalStateException("Already started");
    }
  }

}
