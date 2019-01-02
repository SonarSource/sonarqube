/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.platform.Server;
import org.sonar.api.platform.ServerStartHandler;
import org.sonar.api.platform.ServerStopHandler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class ServerLifecycleNotifierTest {

  private Server server;
  private ServerStartHandler start1;
  private ServerStartHandler start2;
  private ServerStopHandler stop1;
  private ServerStopHandler stop2;

  @Before
  public void before() {
    server = new FakeServer();
    start1 = mock(ServerStartHandler.class);
    start2 = mock(ServerStartHandler.class);
    stop1 = mock(ServerStopHandler.class);
    stop2 = mock(ServerStopHandler.class);
  }

  /**
   * see the explanation in the method ServerLifecycleNotifier.start()
   */
  @Test
  public void doNotNotifyWithTheStartMethod() {
    ServerLifecycleNotifier notifier = new ServerLifecycleNotifier(server, new ServerStartHandler[]{start1, start2}, new ServerStopHandler[]{stop2});
    notifier.start();

    verify(start1, never()).onServerStart(server);
    verify(start2, never()).onServerStart(server);
    verify(stop1, never()).onServerStop(server);
  }

  @Test
  public void notifyOnStart() {
    ServerLifecycleNotifier notifier = new ServerLifecycleNotifier(server, new ServerStartHandler[]{start1, start2}, new ServerStopHandler[]{stop2});
    notifier.notifyStart();

    verify(start1).onServerStart(server);
    verify(start2).onServerStart(server);
    verify(stop1, never()).onServerStop(server);
  }


  @Test
  public void notifyOnStop() {
    ServerLifecycleNotifier notifier = new ServerLifecycleNotifier(server, new ServerStartHandler[]{start1, start2}, new ServerStopHandler[]{stop1, stop2});
    notifier.stop();

    verify(start1, never()).onServerStart(server);
    verify(start2, never()).onServerStart(server);
    verify(stop1).onServerStop(server);
    verify(stop2).onServerStop(server);
  }
}

class FakeServer extends Server {

  @Override
  public String getId() {
    return null;
  }

  @Override
  public String getVersion() {
    return null;
  }

  @Override
  public Date getStartedAt() {
    return null;
  }

  @Override
  public File getRootDir() {
    return null;
  }

  @Override
  public String getContextPath() {
    return null;
  }

  @Override
  public String getPublicRootUrl() {
    return null;
  }

  @Override
  public boolean isDev() {
    return false;
  }

  @Override
  public boolean isSecured() {
    return false;
  }

  @Override
  public String getURL() {
    return null;
  }

  @Override
  public String getPermanentServerId() {
    return null;
  }
}
