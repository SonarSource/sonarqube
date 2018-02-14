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
package org.sonarqube.qa.util;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;
import org.sonarqube.ws.Webhooks.CreateWsResponse.Webhook;
import org.sonarqube.ws.Webhooks.Delivery;
import org.sonarqube.ws.client.webhooks.CreateRequest;
import org.sonarqube.ws.client.webhooks.DeleteRequest;
import org.sonarqube.ws.client.webhooks.DeliveriesRequest;
import org.sonarqube.ws.client.webhooks.DeliveryRequest;
import org.sonarqube.ws.client.webhooks.ListRequest;
import org.sonarqube.ws.client.webhooks.WebhooksService;

import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class WebhookTester {
  private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

  private final TesterSession session;

  WebhookTester(TesterSession session) {
    this.session = session;
  }

  public WebhooksService service() {
    return session.wsClient().webhooks();
  }

  public Webhook generate(Consumer<CreateRequest>... populators) {
    return generate(null, null, populators);
  }

  public Webhook generate(Organization organization, Consumer<CreateRequest>... populators) {
    return generate(organization, null, populators);
  }

  public Webhook generate(Project project, Consumer<CreateRequest>... populators) {
    return generate(null, project, populators);
  }

  @SafeVarargs
  public final Webhook generate(
    @Nullable Organization organization,
    @Nullable Project project,
    Consumer<CreateRequest>... populators
  ) {
    int id = ID_GENERATOR.getAndIncrement();
    CreateRequest request = new CreateRequest()
      .setName("Webhook " + id)
      .setUrl("https://webhook-" + id)
      .setProject(project != null ? project.getKey(): null)
      .setOrganization(organization != null ? organization.getKey() : null);
    stream(populators).forEach(p -> p.accept(request));
    return service().create(request).getWebhook();
  }

  public void deleteAllGlobal() {
    service().list(new ListRequest()).getWebhooksList().forEach(p ->
      service().delete(new DeleteRequest().setWebhook(p.getKey()))
    );
  }

  public List<Delivery> getPersistedDeliveries(Project project) {
    DeliveriesRequest deliveriesReq = new DeliveriesRequest().setComponentKey(project.getKey());
    return service().deliveries(deliveriesReq).getDeliveriesList();
  }

  public Delivery getPersistedDeliveryByName(Project project, String webhookName) {
    List<Delivery> deliveries = getPersistedDeliveries(project);
    Optional<Delivery> delivery = deliveries.stream().filter(d -> d.getName().equals(webhookName)).findFirst();
    assertThat(delivery).isPresent();
    return delivery.get();
  }

  public Delivery getDetailOfPersistedDelivery(Delivery delivery) {
    Delivery detail = service().delivery(new DeliveryRequest().setDeliveryId(delivery.getId())).getDelivery();
    return requireNonNull(detail);
  }

  public void assertThatPersistedDeliveryIsValid(Delivery delivery, @Nullable Project project, @Nullable String url) {
    assertThat(delivery.getId()).isNotEmpty();
    assertThat(delivery.getName()).isNotEmpty();
    assertThat(delivery.hasSuccess()).isTrue();
    assertThat(delivery.getHttpStatus()).isGreaterThanOrEqualTo(200);
    assertThat(delivery.getDurationMs()).isGreaterThanOrEqualTo(0);
    assertThat(delivery.getAt()).isNotEmpty();
    if (project != null) {
      assertThat(delivery.getComponentKey()).isEqualTo(project.getKey());
    }
    if (url != null) {
      assertThat(delivery.getUrl()).startsWith(url);
    }
  }
}
