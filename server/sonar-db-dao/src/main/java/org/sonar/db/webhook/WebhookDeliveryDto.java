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

public class WebhookDeliveryDto extends WebhookDeliveryLiteDto<WebhookDeliveryDto> {
  /** Error message if HTTP request cannot be sent, else null */
  private String errorStacktrace;
  /** The payload that has been sent, cannot be null */
  private String payload;

  @CheckForNull
  public String getErrorStacktrace() {
    return errorStacktrace;
  }

  public WebhookDeliveryDto setErrorStacktrace(@Nullable String s) {
    this.errorStacktrace = s;
    return this;
  }

  public String getPayload() {
    return payload;
  }

  public WebhookDeliveryDto setPayload(String s) {
    this.payload = s;
    return this;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("uuid", uuid)
      .append("componentUuid", componentUuid)
      .append("name", name)
      .append("success", success)
      .append("httpStatus", httpStatus)
      .append("durationMs", durationMs)
      .append("url", url)
      .append("errorStacktrace", errorStacktrace)
      .append("createdAt", createdAt)
      .toString();
  }
}
