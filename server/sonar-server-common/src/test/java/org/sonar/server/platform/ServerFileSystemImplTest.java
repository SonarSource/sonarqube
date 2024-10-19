/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.Configuration;
import org.sonar.api.testfixtures.log.LogTester;

import static org.assertj.core.api.Assertions.assertThat;

public class ServerFileSystemImplTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();

  private ServerFileSystemImpl underTest;
  private File homeDir;
  private File tempDir;
  private File dataDir;

  @Before
  public void before() throws IOException {
    DumpMapConfiguration configuration = new DumpMapConfiguration();
    homeDir = temporaryFolder.newFolder();
    tempDir = temporaryFolder.newFolder();
    dataDir = temporaryFolder.newFolder();
    configuration.put("sonar.path.home", homeDir.toPath().toString());
    configuration.put("sonar.path.temp", tempDir.toPath().toString());
    configuration.put("sonar.path.data", dataDir.toPath().toString());
    underTest = new ServerFileSystemImpl(configuration);
  }

  @Test
  public void start_should_log() {
    underTest.start();
    underTest.stop();
    assertThat(logTester.logs())
      .contains("SonarQube home: " + homeDir.toPath().toString());
  }

  @Test
  public void verify_values_set() {
    assertThat(underTest.getHomeDir()).isEqualTo(homeDir);
    assertThat(underTest.getTempDir()).isEqualTo(tempDir);

    assertThat(underTest.getDeployedPluginsDir()).isEqualTo(new File(dataDir.getAbsolutePath() + "/web/deploy/plugins"));
    assertThat(underTest.getDownloadedPluginsDir()).isEqualTo(new File(homeDir.getAbsolutePath() + "/extensions/downloads"));
    assertThat(underTest.getInstalledBundledPluginsDir()).isEqualTo(new File(homeDir.getAbsolutePath() + "/lib/extensions"));
    assertThat(underTest.getInstalledExternalPluginsDir()).isEqualTo(new File(homeDir.getAbsolutePath() + "/extensions/plugins"));

    assertThat(underTest.getUninstalledPluginsDir()).isEqualTo(new File(tempDir.getAbsolutePath() + "/uninstalled-plugins"));
  }

  private static class DumpMapConfiguration implements Configuration {
    private final Map<String, String> keyValues = new HashMap<>();

    public Configuration put(String key, String value) {
      keyValues.put(key, value.trim());
      return this;
    }

    @Override
    public Optional<String> get(String key) {
      return Optional.ofNullable(keyValues.get(key));
    }

    @Override
    public boolean hasKey(String key) {
      throw new UnsupportedOperationException("hasKey not implemented");
    }

    @Override
    public String[] getStringArray(String key) {
      throw new UnsupportedOperationException("getStringArray not implemented");
    }
  }

}
