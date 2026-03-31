/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.platform.web;

import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.ws.ConcurrentCallsLimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConcurrentCallsLimitInterceptorTest {

  private final ConcurrentCallsLimitInterceptor underTest = new ConcurrentCallsLimitInterceptor();

  private static WebService.Action mockAction(RequestHandler handler) {
    WebService.Action action = mock();
    when(action.path()).thenReturn("api/test");
    when(action.key()).thenReturn("do");
    when(action.handler()).thenReturn(handler);
    return action;
  }

  @Test
  public void preAction_whenHandlerHasNoAnnotation_doesNothing() {
    RequestHandler handler = mock();
    WebService.Action action = mockAction(handler);
    Request request = mock(Request.class);

    underTest.preAction(action, request);
    assertThatNoException();
  }

  @Test
  public void postAction_whenNoSemaphoreAcquired_doesNothing() {
    RequestHandler handler = mock();
    WebService.Action action = mockAction(handler);
    Request request = mock();

    // should not throw even when called without a prior preAction
    underTest.postAction(action, request);
    assertThatNoException();
  }

  @Test
  public void preAction_whenWithinLimit_succeeds() {
    WebService.Action action = mockAction(new LimitedHandler());
    Request request = mock();

    underTest.preAction(action, request);
    underTest.postAction(action, request);
    assertThatNoException();
  }

  @Test
  public void preAction_whenLimitExceeded_throws503() {
    WebService.Action action = mockAction(new LimitOneHandler());
    Request request = mock();

    underTest.preAction(action, request);

    assertThatThrownBy(() -> underTest.preAction(action, request))
      .isInstanceOf(ServerException.class)
      .satisfies(e -> assertThat(((ServerException) e).httpCode()).isEqualTo(503));
  }

  @Test
  public void postAction_releasesSlot_allowingSubsequentCall() {
    WebService.Action action = mockAction(new LimitOneHandler());
    Request request = mock();

    underTest.preAction(action, request);
    underTest.postAction(action, request);

    // slot was released, next call should succeed
    underTest.preAction(action, request);
    underTest.postAction(action, request);
    assertThatNoException();
  }

  @Test
  public void preAction_whenLimitExceeded_doesNotAcquireSemaphore() {
    WebService.Action action = mockAction(new LimitOneHandler());
    Request request = mock();

    // first call acquires the only slot
    underTest.preAction(action, request);

    // second call fails - limit reached, no semaphore acquired
    assertThatThrownBy(() -> underTest.preAction(action, request))
      .isInstanceOf(ServerException.class);

    // slot is still held by the first call; calling postAction releases it
    underTest.postAction(action, request);

    // slot is now free; a new call should succeed
    underTest.preAction(action, request);
    underTest.postAction(action, request);
    assertThatNoException();
  }

  @ConcurrentCallsLimit(5)
  static class LimitedHandler implements RequestHandler {
    @Override
    public void handle(Request request, Response response) {
      // nothing to do
    }
  }

  @ConcurrentCallsLimit(1)
  static class LimitOneHandler implements RequestHandler {
    @Override
    public void handle(Request request, Response response) {
      // nothing to do
    }
  }
}
