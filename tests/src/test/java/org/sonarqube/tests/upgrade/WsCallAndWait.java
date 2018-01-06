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
package org.sonarqube.tests.upgrade;

import com.sonar.orchestrator.Orchestrator;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.sonar.wsclient.jsonsimple.JSONObject;
import org.sonar.wsclient.jsonsimple.parser.JSONParser;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;

public abstract class WsCallAndWait<RESPONSE> {
  private static final long TIMEOUT_5_MINUTES = 5L * 60 * 1000;
  private static final long DELAY_3_SECONDS = 3L * 1000;

  private final Orchestrator orchestrator;
  private final String targetRelativeUrl;
  private final long timeout;
  private final long delay;

  protected WsCallAndWait(Orchestrator orchestrator, String targetRelativeUrl, long timeout, long delay) {
    this.orchestrator = orchestrator;
    this.targetRelativeUrl = checkNotNull(targetRelativeUrl);
    this.timeout = timeout;
    this.delay = delay;
  }

  protected WsCallAndWait(Orchestrator orchestrator, String targetRelativeUrl) {
    this(orchestrator, targetRelativeUrl, TIMEOUT_5_MINUTES, DELAY_3_SECONDS);
  }

  @Nonnull
  public RESPONSE call() {
    String response = orchestrator.getServer().wsClient().post(targetRelativeUrl);
    JSONObject jsonObject = toJsonObject(response);
    try {
      return parse(jsonObject);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to parse JSON response", e);
    }
  }

  @CheckForNull
  public RESPONSE callAndWait() {
    long endAt = System.currentTimeMillis() + timeout;

    while (System.currentTimeMillis() < endAt) {
      RESPONSE response = call();
      if (shouldWait(response)) {
        sleepQuietly(delay);
      } else {
        return response;
      }
    }
    return null;
  }

  private void sleepQuietly(long rateInMs) {
    try {
      Thread.sleep(rateInMs);
    } catch (InterruptedException e) {
      propagate(e);
    }
  }

  private JSONObject toJsonObject(String s) {
    try {
      JSONParser parser = new JSONParser();
      Object o = parser.parse(s);
      if (o instanceof JSONObject) {
        return (JSONObject) o;
      }
      throw new RuntimeException("Can not parse response from server migration WS (not a JSON object)");
    } catch (Exception e) {
      throw new IllegalStateException("Invalid JSON: " + s, e);
    }
  }

  @Nonnull
  protected abstract RESPONSE parse(JSONObject jsonObject);

  protected abstract boolean shouldWait(RESPONSE response);
}
