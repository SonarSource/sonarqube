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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.ws.ActionInterceptor;
import org.sonar.server.ws.ConcurrentCallsLimit;

/**
 * Enforces the {@link ConcurrentCallsLimit} annotation on web service actions.
 * When the maximum number of concurrent calls is reached, returns HTTP 503.
 */
public class ConcurrentCallsLimitInterceptor implements ActionInterceptor {

  private final ConcurrentHashMap<String, Semaphore> semaphores = new ConcurrentHashMap<>();
  private final ThreadLocal<Semaphore> acquiredSemaphore = new ThreadLocal<>();

  @Override
  public void preAction(WebService.Action action, Request request) {
    ConcurrentCallsLimit annotation = action.handler().getClass().getAnnotation(ConcurrentCallsLimit.class);
    if (annotation == null) {
      return;
    }
    String key = action.path() + "/" + action.key();
    Semaphore semaphore = semaphores.computeIfAbsent(key, k -> new Semaphore(annotation.value()));
    if (semaphore.tryAcquire()) {
      acquiredSemaphore.set(semaphore);
    } else {
      throw new ServerException(503, "The maximum number of concurrent calls for this web service has been reached");
    }
  }

  @Override
  public void postAction(WebService.Action action, Request request) {
    Semaphore semaphore = acquiredSemaphore.get();
    if (semaphore != null) {
      semaphore.release();
      acquiredSemaphore.remove();
    }
  }
}
