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
package org.sonar.db.webhook;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.builder.ToStringBuilder;

public class WebhookDeliveryLiteDto<T extends WebhookDeliveryLiteDto> {
  /** Technical unique identifier, can't be null */
  protected String uuid;
  /** Technical unique identifier, can be null for migration */
  protected String webhookUuid;
  /** Component UUID, can't be null */
  protected String componentUuid;
  /** Compute Engine task UUID, can be null */
  protected String ceTaskUuid;
  /** analysis UUID, can be null */
  protected String analysisUuid;
  /** Name, can't be null */
  protected String name;
  protected boolean success;
  /** HTTP response status. Null if HTTP request cannot be sent */
  protected Integer httpStatus;
  /** Duration in ms. Null if HTTP request cannot be sent */
  protected Integer durationMs;
  /** URL, cannot be null */
  protected String url;
  /** Time of delivery */
  protected long createdAt;

  public String getUuid() {
    return uuid;
  }

  public T setUuid(String s) {
    this.uuid = s;
    return (T) this;
  }

  public String getWebhookUuid() {
    return webhookUuid;
  }

  public T setWebhookUuid(String webhookUuid) {
    this.webhookUuid = webhookUuid;
    return (T) this;
  }

  public String getComponentUuid() {
    return componentUuid;
  }

  public T setComponentUuid(String s) {
    this.componentUuid = s;
    return (T) this;
  }

  @CheckForNull
  public String getCeTaskUuid() {
    return ceTaskUuid;
  }

  public T setCeTaskUuid(@Nullable String s) {
    this.ceTaskUuid = s;
    return (T) this;
  }

  @CheckForNull
  public String getAnalysisUuid() {
    return analysisUuid;
  }

  public T setAnalysisUuid(@Nullable String s) {
    this.analysisUuid = s;
    return (T) this;
  }

  public String getName() {
    return name;
  }

  public T setName(String s) {
    this.name = s;
    return (T) this;
  }

  public boolean isSuccess() {
    return success;
  }

  public T setSuccess(boolean b) {
    this.success = b;
    return (T) this;
  }

  @CheckForNull
  public Integer getHttpStatus() {
    return httpStatus;
  }

  public T setHttpStatus(@Nullable Integer i) {
    this.httpStatus = i;
    return (T) this;
  }

  @CheckForNull
  public Integer getDurationMs() {
    return durationMs;
  }

  public T setDurationMs(@Nullable Integer i) {
    this.durationMs = i;
    return (T) this;
  }

  public String getUrl() {
    return url;
  }

  public T setUrl(String s) {
    this.url = s;
    return (T) this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public T setCreatedAt(long l) {
    this.createdAt = l;
    return (T) this;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("uuid", uuid)
      .append("componentUuid", componentUuid)
      .append("ceTaskUuid", ceTaskUuid)
      .append("name", name)
      .append("success", success)
      .append("httpStatus", httpStatus)
      .append("durationMs", durationMs)
      .append("url", url)
      .append("createdAt", createdAt)
      .toString();
  }
}
