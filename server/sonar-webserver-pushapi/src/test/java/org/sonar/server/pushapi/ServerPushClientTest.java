/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncListener;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ServerPushClientTest {

  private final ScheduledExecutorService executorService = mock(ScheduledExecutorService.class);
  private final AsyncContext asyncContext = mock(AsyncContext.class);

  private final ServerPushClient underTest = new ServerPushClient(executorService, asyncContext) {};

  private final ServletOutputStream outputStream = mock(ServletOutputStream.class);
  private ServletResponse servletResponse;

  @Before
  public void before() throws IOException {
    servletResponse = mock(ServletResponse.class);

    when(servletResponse.getOutputStream()).thenReturn(outputStream);
    when(asyncContext.getResponse()).thenReturn(servletResponse);
    when(asyncContext.getRequest()).thenReturn(mock(ServletRequest.class));
  }

  @Test
  public void scheduleHeartbeat_oneTaskIsScheduled() {
    underTest.scheduleHeartbeat();

    verify(executorService, Mockito.times(1))
      .schedule(any(HeartbeatTask.class), anyLong(), any());
  }

  @Test
  public void writeAndFlush_writeIsCalledOnceAndFlushIsCalledOnce() throws IOException {
    underTest.writeAndFlush('a');

    verify(outputStream, Mockito.times(1)).flush();
    verify(outputStream, Mockito.times(1)).write('a');
  }

  @Test
  public void write_writeIsCalledOnceAndDoesntFlush() throws IOException {
    underTest.write('a');

    verify(outputStream, Mockito.never()).flush();
    verify(outputStream, Mockito.times(1)).write('a');
  }

  @Test
  public void flush_streamIsFlushed() throws IOException {
    underTest.flush();

    verify(outputStream, Mockito.only()).flush();
  }

  @Test
  public void addListener_addsListener() {
    AsyncListener mock = mock(AsyncListener.class);

    underTest.addListener(mock);

    verify(asyncContext).addListener(mock);
  }

  @Test
  public void write_exceptionCausesConnectionToClose() throws IOException {
    when(servletResponse.getOutputStream()).thenThrow(new IOException("mock exception"));
    ScheduledFuture task = mock(ScheduledFuture.class);
    when(executorService.schedule(any(HeartbeatTask.class), anyLong(), any(TimeUnit.class))).thenReturn(task);
    underTest.scheduleHeartbeat();

    underTest.write('a');

    verify(asyncContext).complete();
  }

  @Test
  public void flush_exceptionCausesConnectionToClose() throws IOException {
    when(servletResponse.getOutputStream()).thenThrow(new IOException("mock exception"));
    ScheduledFuture task = mock(ScheduledFuture.class);
    when(executorService.schedule(any(HeartbeatTask.class), anyLong(), any(TimeUnit.class))).thenReturn(task);
    underTest.scheduleHeartbeat();

    underTest.flush();

    verify(asyncContext).complete();
  }
}
