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
package org.sonar.server.webhook;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public class TestWebhookCaller implements WebhookCaller {

  private final Queue<Item> deliveries = new LinkedList<>();
  private final AtomicInteger countSent = new AtomicInteger(0);

  public TestWebhookCaller enqueueSuccess(long at, int httpCode, int durationMs) {
    deliveries.add(new Item(at, httpCode, durationMs, null));
    return this;
  }

  public TestWebhookCaller enqueueFailure(long at, Throwable t) {
    deliveries.add(new Item(at, null, null, t));
    return this;
  }

  @Override
  public WebhookDelivery call(Webhook webhook, WebhookPayload payload) {
    Item item = requireNonNull(deliveries.poll(), "Queue is empty");
    countSent.incrementAndGet();
    return new WebhookDelivery.Builder()
      .setAt(item.at)
      .setHttpStatus(item.httpCode)
      .setDurationInMs(item.durationMs)
      .setError(item.throwable)
      .setPayload(payload)
      .setWebhook(webhook)
      .build();
  }

  public int countSent() {
    return countSent.get();
  }

  private static class Item {
    final long at;
    final Integer httpCode;
    final Integer durationMs;
    final Throwable throwable;

    Item(long at, @Nullable Integer httpCode, @Nullable Integer durationMs, @Nullable Throwable throwable) {
      this.at = at;
      this.httpCode = httpCode;
      this.durationMs = durationMs;
      this.throwable = throwable;
    }
  }
}
