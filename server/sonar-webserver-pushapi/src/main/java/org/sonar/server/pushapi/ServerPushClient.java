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
package org.sonar.server.pushapi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncListener;
import javax.servlet.ServletOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ServerPushClient {

  private static final Logger LOG = LoggerFactory.getLogger(ServerPushClient.class);
  private static final int DEFAULT_HEARTBEAT_PERIOD = 20;

  protected final AsyncContext asyncContext;

  private final ScheduledExecutorService executorService;
  private final HeartbeatTask heartbeatTask;
  private ScheduledFuture<?> startedHeartbeat;

  protected ServerPushClient(ScheduledExecutorService executorService, AsyncContext asyncContext) {
    this.executorService = executorService;
    this.asyncContext = asyncContext;
    this.heartbeatTask = new HeartbeatTask(this);
  }

  public void scheduleHeartbeat() {
    startedHeartbeat = executorService.schedule(heartbeatTask, DEFAULT_HEARTBEAT_PERIOD, TimeUnit.SECONDS);
  }

  public void writeAndFlush(String payload) throws IOException {
    payload = ensureCorrectMessageEnding(payload);
    output().write(payload.getBytes(StandardCharsets.UTF_8));
    flush();
  }

  private static String ensureCorrectMessageEnding(String payload) {
    return payload.endsWith("\n\n") ? payload : (payload + "\n\n");
  }

  public void writeAndFlush(char character) {
    write(character);
    flush();
  }

  public synchronized void write(char character) {
    try {
      output().write(character);
    } catch (IOException e) {
      handleIOException(e);
    }
  }

  public synchronized void flush() {
    try {
      output().flush();
    } catch (IOException e) {
      handleIOException(e);
    }
  }

  private void handleIOException(IOException e) {
    String remoteAddr = asyncContext.getRequest().getRemoteAddr();
    LOG.debug(String.format("The server push client %s gone without notice, closing the connection (%s)", remoteAddr, e.getMessage()));
    throw new IllegalStateException(e.getMessage());
  }

  public synchronized void close() {
    startedHeartbeat.cancel(false);
    try {
      asyncContext.complete();
    } catch (IllegalStateException ex) {
      LOG.trace("Push connection was already closed");
    }
  }

  private ServletOutputStream output() throws IOException {
    return asyncContext.getResponse().getOutputStream();
  }

  public void addListener(AsyncListener asyncListener) {
    asyncContext.addListener(asyncListener);
  }

}
