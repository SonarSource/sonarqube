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
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.server.async.AsyncExecution;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class AsynchronousWebHooksImplTest {
  private static final long NOW = 1_500_000_000_000L;
  private static final String PROJECT_UUID = "P1_UUID";

  private final MapSettings settings = new MapSettings();
  private final TestWebhookCaller caller = new TestWebhookCaller();
  private final WebhookDeliveryStorage deliveryStorage = mock(WebhookDeliveryStorage.class);
  private final WebhookPayload mock = mock(WebhookPayload.class);
  private final RecordingAsyncExecution asyncExecution = new RecordingAsyncExecution();

  private final WebHooksImpl underTest = new WebHooksImpl(caller, deliveryStorage, asyncExecution);

  @Test
  public void send_global_webhooks() {
    settings.setProperty("sonar.webhooks.global", "1,2");
    settings.setProperty("sonar.webhooks.global.1.name", "First");
    settings.setProperty("sonar.webhooks.global.1.url", "http://url1");
    settings.setProperty("sonar.webhooks.global.2.name", "Second");
    settings.setProperty("sonar.webhooks.global.2.url", "http://url2");
    caller.enqueueSuccess(NOW, 200, 1_234);
    caller.enqueueFailure(NOW, new IOException("Fail to connect"));

    underTest.sendProjectAnalysisUpdate(settings.asConfig(), new WebHooks.Analysis(PROJECT_UUID, "1", "#1"), () -> mock);

    assertThat(caller.countSent()).isZero();
    verifyZeroInteractions(deliveryStorage);

    asyncExecution.executeRecorded();

    assertThat(caller.countSent()).isEqualTo(2);
    verify(deliveryStorage, times(2)).persist(any(WebhookDelivery.class));
    verify(deliveryStorage).purge(PROJECT_UUID);
  }

  private static class RecordingAsyncExecution implements AsyncExecution {
    private final List<Runnable> runnableList = new ArrayList<>();

    @Override
    public void addToQueue(Runnable r) {
      runnableList.add(requireNonNull(r));
    }

    public void executeRecorded() {
      ArrayList<Runnable> runnables = new ArrayList<>(runnableList);
      runnableList.clear();
      runnables.forEach(Runnable::run);
    }
  }
}
