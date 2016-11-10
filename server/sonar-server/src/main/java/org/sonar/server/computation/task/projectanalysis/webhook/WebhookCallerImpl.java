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

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.utils.System2;

@ComputeEngineSide
public class WebhookCallerImpl implements WebhookCaller {

  private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
  private static final String PROJECT_KEY_HEADER = "X-SonarQube-Project";

  private final System2 system;
  private final OkHttpClient okHttpClient;

  public WebhookCallerImpl(System2 system, OkHttpClient okHttpClient) {
    this.system = system;
    this.okHttpClient = okHttpClient;
  }

  @Override
  public WebhookDelivery call(Webhook webhook, WebhookPayload payload) {
    WebhookDelivery.Builder builder = new WebhookDelivery.Builder();
    long startedAt = system.now();
    builder
      .setAt(startedAt)
      .setPayload(payload)
      .setWebhook(webhook);

    try {
      Request request = buildHttpRequest(webhook, payload);
      try (Response response = okHttpClient.newCall(request).execute()) {
        builder.setHttpStatus(response.code());
        builder.setDurationInMs((int) (system.now() - startedAt));
      }
    } catch (Exception e) {
      builder.setError(e);
    }
    return builder.build();
  }

  private static Request buildHttpRequest(Webhook webhook, WebhookPayload payload) {
    Request.Builder request = new Request.Builder();
    request.url(webhook.getUrl());
    request.header(PROJECT_KEY_HEADER, payload.getProjectKey());
    RequestBody body = RequestBody.create(JSON, payload.toJson());
    request.post(body);
    return request.build();
  }

}
