/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.server.tester;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.rules.ExternalResource;
import org.sonar.api.database.DatabaseProperties;
import org.sonar.api.resources.Language;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.process.ProcessEntryPoint;
import org.sonar.process.ProcessProperties;
import org.sonar.server.es.EsServerHolder;
import org.sonar.server.platform.BackendCleanup;
import org.sonar.server.platform.Platform;
import org.sonar.server.platform.ServerTesterPlatform;
import org.sonar.server.plugins.UpdateCenterClient;
import org.sonar.server.ws.WsTester;

import static org.sonar.server.platform.Platform.Startup.ALL;
import static org.sonar.server.platform.Platform.Startup.NO_STARTUP_TASKS;

/**
 * Entry point to implement medium tests of server components.
 * <p/>
 * The system properties starting with "mediumTests." override the programmatic settings, for example:
 * <code>-DmediumTests.sonar.log.level=TRACE</code>
 *
 * @since 4.4
 */
public class ServerTester extends ExternalResource {

  private static final Logger LOG = Loggers.get(ServerTester.class);
  private static final String PROP_PREFIX = "mediumTests.";

  private ServerTesterPlatform platform;
  private EsServerHolder esServerHolder;
  private final File homeDir = newTempDir("tmp-sq-");
  private final List<Object> components = Lists.newArrayList(WsTester.class);
  private final Properties initialProps = new Properties();
  private final ServletContext servletContext = new AttributeHolderServletContext();
  private URL updateCenterUrl;
  private boolean startupTasks = false;
  private boolean esIndexes = false;

  public ServerTester withStartupTasks() {
    this.startupTasks = true;
    return this;
  }

  public ServerTester withEsIndexes() {
    this.esIndexes = true;
    return this;
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

    try {
      Properties properties = new Properties();
      properties.putAll(initialProps);
      esServerHolder = EsServerHolder.get();
      properties.setProperty(ProcessProperties.CLUSTER_NAME, esServerHolder.getClusterName());
      properties.setProperty(ProcessProperties.SEARCH_PORT, String.valueOf(esServerHolder.getPort()));
      properties.setProperty(ProcessProperties.SEARCH_HOST, esServerHolder.getAddress().getHostAddress());
      properties.setProperty(ProcessProperties.PATH_HOME, homeDir.getAbsolutePath());
      properties.setProperty(ProcessProperties.PATH_DATA, new File(homeDir, "data").getAbsolutePath());
      File temporaryFolderIn = createTemporaryFolderIn();
      properties.setProperty(ProcessProperties.PATH_TEMP, temporaryFolderIn.getAbsolutePath());
      properties.setProperty(ProcessEntryPoint.PROPERTY_SHARED_PATH, temporaryFolderIn.getAbsolutePath());
      properties.setProperty(ProcessEntryPoint.PROPERTY_PROCESS_INDEX, "2");
      properties.setProperty(DatabaseProperties.PROP_URL, "jdbc:h2:" + homeDir.getAbsolutePath() + "/h2");
      if (updateCenterUrl != null) {
        properties.setProperty(UpdateCenterClient.URL_PROPERTY, updateCenterUrl.toString());
      }
      for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
        String key = entry.getKey().toString();
        if (key.startsWith(PROP_PREFIX)) {
          properties.put(StringUtils.substringAfter(key, PROP_PREFIX), entry.getValue());
        }
      }
      if (!esIndexes) {
        properties.put("sonar.internal.es.disableIndexes", true);
      }
      platform = new ServerTesterPlatform(() -> new Platform.AutoStarter() {
        private boolean running = false;

        @Override
        public void execute(Runnable startCode) {
          running = true;
          startCode.run();
        }

        @Override
        public void failure(Throwable t) {
          stop();
          Throwables.propagate(t);
          this.running = false;
        }

        @Override
        public void success() {
          this.running = false;
        }

        @Override
        public boolean isRunning() {
          return this.running;
        }

        @Override
        public void abort() {
          // do nothing specific
        }

        @Override
        public boolean isAborting() {
          return false;
        }

        @Override
        public void aborted() {
          // do nothing specific
        }
      });
      platform.init(properties, servletContext);
      platform.addComponents(components);
      platform.doStart(startupTasks ? ALL : NO_STARTUP_TASKS);
    } catch (Exception e) {
      stop();
      Throwables.propagate(e);
    }
    if (!platform.isStarted()) {
      throw new IllegalStateException("Server not started. You should check that db migrations " +
        "are correctly declared, for example in schema-h2.sql or DatabaseVersion");
    }
  }

  private File createTemporaryFolderIn() throws IOException {
    File createdFolder = File.createTempFile("ServerTester", "");
    createdFolder.delete();
    createdFolder.mkdir();
    return createdFolder;
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
    try {
      if (platform != null) {
        platform.doStop();
        platform = null;
      }
    } catch (Exception e) {
      LOG.error("Fail to stop web server", e);
    }
    esServerHolder = null;
    FileUtils.deleteQuietly(homeDir);
  }

  public void restart() {
    platform.restart();
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

  public ServerTester addXoo() {
    addComponents(Xoo.class);
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

  public ServerTester setUpdateCenterUrl(URL url) {
    this.updateCenterUrl = url;
    return this;
  }

  /**
   * Set a property available for startup. Must be called before {@link #start()}. Does not affect
   * Elasticsearch server.
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

  public ComponentContainer getContainer() {
    return platform.getContainer();
  }

  public WsTester wsTester() {
    return get(WsTester.class);
  }

  public EsServerHolder getEsServerHolder() {
    return esServerHolder;
  }

  private void checkStarted() {
    if (platform == null || !platform.isStarted()) {
      throw new IllegalStateException("Not started");
    }
  }

  private void checkNotStarted() {
    if (platform != null && platform.isStarted()) {
      throw new IllegalStateException("Already started");
    }
  }

  public static class Xoo implements Language {

    public static final String KEY = "xoo";
    public static final String NAME = "Xoo";
    public static final String FILE_SUFFIX = ".xoo";

    private static final String[] XOO_SUFFIXES = {
      FILE_SUFFIX
    };

    @Override
    public String getKey() {
      return KEY;
    }

    @Override
    public String getName() {
      return NAME;
    }

    @Override
    public String[] getFileSuffixes() {
      return XOO_SUFFIXES;
    }
  }

  private static File newTempDir(String prefix) {
    try {
      return Files.createTempDirectory(prefix).toFile();
    } catch (Exception e) {
      throw new IllegalStateException("Fail to create temp dir", e);
    }
  }
}
