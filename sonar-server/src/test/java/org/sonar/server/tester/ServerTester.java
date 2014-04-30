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

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.junit.rules.ExternalResource;
import org.sonar.api.CoreProperties;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.server.platform.Platform;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Entry point to implement medium tests of server components
 *
 * @since 4.4
 */
public class ServerTester extends ExternalResource {

  private Platform platform;
  private File tempDir;
  private List components = Lists.newArrayList(DataStoreCleanup.class);
  private final Properties initialProps = new Properties();

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
    tempDir = createTempDir();
    Properties properties = new Properties();
    properties.putAll(initialProps);
    properties.setProperty(CoreProperties.SONAR_HOME, tempDir.getAbsolutePath());
    properties.setProperty(DatabaseProperties.PROP_URL, "jdbc:h2:" + tempDir.getAbsolutePath() + "/h2");
    platform = new Platform();
    platform.init(properties);
    platform.addComponents(components);
    platform.doStart();
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
    if (platform != null) {
      platform.doStop();
      platform = null;
    }
    FileUtils.deleteQuietly(tempDir);
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

  /**
   * Set a property available for startup. Must be called before {@link #start()}.
   */
  public ServerTester setProperty(String key, String value) {
    checkNotStarted();
    initialProps.setProperty(key, value);
    return this;
  }

  /**
   * Truncate all db tables and es indices
   */
  public void clearDataStores() {
    checkStarted();
    get(DataStoreCleanup.class).clear();
  }

  /**
   * Get a component from the platform
   */
  public <C> C get(Class<C> component) {
    checkStarted();
    return platform.getContainer().getComponentByType(component);
  }

  private void checkStarted() {
    if (platform == null) {
      throw new IllegalStateException("Not started");
    }
  }

  private void checkNotStarted() {
    if (platform != null) {
      throw new IllegalStateException("Already started");
    }
  }

}
