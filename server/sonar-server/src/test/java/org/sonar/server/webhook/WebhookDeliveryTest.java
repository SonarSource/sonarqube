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

import java.io.IOException;
import org.junit.Test;
import org.sonar.server.webhook.Webhook;
import org.sonar.server.webhook.WebhookDelivery;
import org.sonar.server.webhook.WebhookPayload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;


public class WebhookDeliveryTest {

  @Test
  public void isSuccess_returns_false_if_failed_to_send_http_request() {
    WebhookDelivery delivery = newBuilderTemplate()
      .setError(new IOException("Fail to connect"))
      .build();

    assertThat(delivery.isSuccess()).isFalse();
  }

  @Test
  public void isSuccess_returns_false_if_http_response_returns_error_status() {
    WebhookDelivery delivery = newBuilderTemplate()
      .setHttpStatus(404)
      .build();

    assertThat(delivery.isSuccess()).isFalse();
  }

  @Test
  public void isSuccess_returns_true_if_http_response_returns_2xx_code() {
    WebhookDelivery delivery = newBuilderTemplate()
      .setHttpStatus(204)
      .build();

    assertThat(delivery.isSuccess()).isTrue();
  }

  @Test
  public void getErrorMessage_returns_empty_if_no_error() {
    WebhookDelivery delivery = newBuilderTemplate().build();

    assertThat(delivery.getErrorMessage()).isEmpty();
  }

  @Test
  public void getErrorMessage_returns_root_cause_message_if_error() {
    Exception rootCause = new IOException("fail to connect");
    Exception cause = new IOException("nested", rootCause);
    WebhookDelivery delivery = newBuilderTemplate()
      .setError(cause)
      .build();

    assertThat(delivery.getErrorMessage().get()).isEqualTo("fail to connect");
  }

  private static WebhookDelivery.Builder newBuilderTemplate() {
    return new WebhookDelivery.Builder()
      .setWebhook(mock(Webhook.class))
      .setPayload(mock(WebhookPayload.class))
      .setAt(1_000L);
  }
}
