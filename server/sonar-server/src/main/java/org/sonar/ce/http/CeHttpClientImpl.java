/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.ce.http;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import org.apache.commons.io.IOUtils;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.process.sharedmemoryfile.DefaultProcessCommands;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

import static java.util.Objects.requireNonNull;
import static org.sonar.process.ProcessEntryPoint.PROPERTY_SHARED_PATH;
import static org.sonar.process.ProcessId.COMPUTE_ENGINE;

/**
 * Client for the HTTP server of the Compute Engine.
 */
public class CeHttpClientImpl implements CeHttpClient {

  private static final String PATH_CHANGE_LOG_LEVEL = "changeLogLevel";
  private static final String PATH_SYSTEM_INFO = "systemInfo";

  private final File ipcSharedDir;

  public CeHttpClientImpl(Configuration config) {
    this.ipcSharedDir = new File(config.get(PROPERTY_SHARED_PATH).get());
  }

  /**
   * Connects to the specified JVM process and requests system information.
   *
   * @return the system info, or absent if the process is not up or if its HTTP URL
   * is not registered into IPC.
   */
  @Override
  public Optional<ProtobufSystemInfo.SystemInfo> retrieveSystemInfo() {
    return call(SystemInfoActionClient.INSTANCE);
  }

  private enum SystemInfoActionClient implements ActionClient<Optional<ProtobufSystemInfo.SystemInfo>> {
    INSTANCE;

    @Override
    public String getPath() {
      return PATH_SYSTEM_INFO;
    }

    @Override
    public Optional<ProtobufSystemInfo.SystemInfo> getDefault() {
      return Optional.empty();
    }

    @Override
    public Optional<ProtobufSystemInfo.SystemInfo> call(String url) throws Exception {
      byte[] protobuf = IOUtils.toByteArray(new URI(url));
      return Optional.of(ProtobufSystemInfo.SystemInfo.parseFrom(protobuf));
    }
  }

  @Override
  public void changeLogLevel(LoggerLevel level) {
    requireNonNull(level, "level can't be null");
    call(new ChangeLogLevelActionClient(level));
  }

  private static final class ChangeLogLevelActionClient implements ActionClient<Void> {
    private final LoggerLevel newLogLevel;

    private ChangeLogLevelActionClient(LoggerLevel newLogLevel) {
      this.newLogLevel = newLogLevel;
    }

    @Override
    public String getPath() {
      return PATH_CHANGE_LOG_LEVEL;
    }

    @Override
    public Void getDefault() {
      return null;
    }

    @Override
    public Void call(String url) throws Exception {
      okhttp3.Request request = new okhttp3.Request.Builder()
        .post(RequestBody.create(null, new byte[0]))
        .url(url + "?level=" + newLogLevel.name())
        .build();
      try (okhttp3.Response response = new OkHttpClient().newCall(request).execute()) {
        if (response.code() != 200) {
          throw new IOException(
            String.format(
              "Failed to change log level in Compute Engine. Code was '%s' and response was '%s' for url '%s'",
              response.code(),
              response.body().string(),
              url));
        }
        return null;
      }
    }
  }

  @Override
  public void refreshCeWorkerCount() {
    call(RefreshCeWorkerCountActionClient.INSTANCE);
  }

  private enum RefreshCeWorkerCountActionClient implements ActionClient<Void> {
    INSTANCE;

    @Override
    public String getPath() {
      return "refreshWorkerCount";
    }

    @Override
    public Void getDefault() {
      return null;
    }

    @Override
    public Void call(String url) throws Exception {
      okhttp3.Request request = new okhttp3.Request.Builder()
        .post(RequestBody.create(null, new byte[0]))
        .url(url)
        .build();
      try (okhttp3.Response response = new OkHttpClient().newCall(request).execute()) {
        if (response.code() != 200) {
          throw new IOException(
            String.format(
              "Failed to trigger refresh of CE Worker count. Code was '%s' and response was '%s' for url '%s'",
              response.code(),
              response.body().string(),
              url));
        }
        return null;
      }
    }
  }

  private <T> T call(ActionClient<T> actionClient) {
    try (DefaultProcessCommands commands = DefaultProcessCommands.secondary(ipcSharedDir, COMPUTE_ENGINE.getIpcIndex())) {
      if (commands.isUp()) {
        return actionClient.call(commands.getHttpUrl() + "/" + actionClient.getPath());
      }
      return actionClient.getDefault();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to call HTTP server of process " + COMPUTE_ENGINE, e);
    }
  }

  private interface ActionClient<T> {
    /**
     * Path of the action.
     */
    String getPath();

    /**
     * Value to return when the Compute Engine is not ready.
     */
    T getDefault();

    /**
     * Delegates to perform the call to the Compute Engine's specified absolute URL.
     */
    T call(String url) throws Exception;
  }
}
