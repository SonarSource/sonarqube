/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.task.projectanalysis.webhook;

import java.util.Optional;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;

/**
 * A {@link WebhookDelivery} represents the result of a webhook call.
 */
@Immutable
public class WebhookDelivery {

  private final Webhook webhook;
  private final WebhookPayload payload;
  private final Integer httpStatus;
  private final Long durationInMs;
  private final long at;
  private final Throwable throwable;

  private WebhookDelivery(Builder builder) {
    this.webhook = requireNonNull(builder.webhook);
    this.payload = requireNonNull(builder.payload);
    this.httpStatus = builder.httpStatus;
    this.durationInMs = builder.durationInMs;
    this.at = builder.at;
    this.throwable = builder.throwable;
  }

  public Webhook getWebhook() {
    return webhook;
  }

  public WebhookPayload getPayload() {
    return payload;
  }

  /**
   * @return the HTTP status if {@link #getThrowable()} is empty, else returns 
   * {@link Optional#empty()}
   */
  public Optional<Integer> getHttpStatus() {
    return Optional.ofNullable(httpStatus);
  }

  /**
   * @return the duration in milliseconds if {@link #getThrowable()} is empty,
   * else returns {@link Optional#empty()}
   */
  public Optional<Long> getDurationInMs() {
    return Optional.ofNullable(durationInMs);
  }

  /**
   * @return the date of sending
   */
  public long getAt() {
    return at;
  }

  /**
   * @return the error raised if the request could not be executed due to a connectivity
   * problem or timeout
   */
  public Optional<Throwable> getThrowable() {
    return Optional.ofNullable(throwable);
  }

  public static class Builder {
    private Webhook webhook;
    private WebhookPayload payload;
    private Integer httpStatus;
    private Long durationInMs;
    private long at;
    private Throwable throwable;

    public Builder setWebhook(Webhook w) {
      this.webhook = w;
      return this;
    }

    public Builder setPayload(WebhookPayload payload) {
      this.payload = payload;
      return this;
    }

    public Builder setHttpStatus(@Nullable Integer httpStatus) {
      this.httpStatus = httpStatus;
      return this;
    }

    public Builder setDurationInMs(@Nullable Long durationInMs) {
      this.durationInMs = durationInMs;
      return this;
    }

    public Builder setAt(long at) {
      this.at = at;
      return this;
    }

    public Builder setThrowable(@Nullable Throwable t) {
      this.throwable = t;
      return this;
    }

    public WebhookDelivery build() {
      return new WebhookDelivery(this);
    }
  }
}
