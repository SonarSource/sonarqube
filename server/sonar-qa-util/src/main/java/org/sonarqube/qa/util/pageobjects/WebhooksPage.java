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
package org.sonarqube.qa.util.pageobjects;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import java.util.Arrays;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class WebhooksPage {

  public WebhooksPage() {
    $(".page-header .page-title").should(exist).shouldHave(text("Webhooks"));
  }

  public WebhooksPage hasWebhook(String text) {
    getWebhooks().find(text(text)).should(exist);
    return this;
  }

  public WebhooksPage hasNoWebhooks() {
    $(".boxed-group").shouldHave(text("No webhook defined"));
    return this;
  }

  public WebhooksPage hasLatestDelivery(String webhookName) {
    getWebhook(webhookName).shouldNotHave(text("Never"));
    return this;
  }

  public WebhooksPage hasNoLatestDelivery(String webhookName) {
    getWebhook(webhookName).shouldHave(text("Never"));
    return this;
  }

  public WebhooksPage countWebhooks(Integer number) {
    getWebhooks().shouldHaveSize(number);
    return this;
  }

  public WebhooksPage createWebhook(String name, String url) {
    $(".js-webhook-create").shouldBe(visible).shouldBe(enabled).shouldNotHave(cssClass("disabled")).click();
    modalShouldBeOpen("Create Webhook");
    $("#webhook-name").shouldBe(visible).sendKeys(name);
    $("#webhook-url").shouldBe(visible).sendKeys(url);
    $("button[type='submit']").shouldBe(visible).click();
    modalShouldBeClosed();
    return this;
  }

  public  WebhooksPage createIsDisabled() {
    $(".js-webhook-create").shouldBe(visible).shouldHave(cssClass("disabled")).click();
    modalShouldBeClosed();
    return this;
  }

  public WebhooksPage deleteWebhook(String webhookName) {
    SelenideElement webhook = getWebhook(webhookName);
    webhook.$(".dropdown-toggle").shouldBe(visible).click();
    webhook.$(".js-webhook-delete").shouldBe(visible).click();
    modalShouldBeOpen("Delete Webhook");
    $("button.button-red").shouldBe(visible).click();
    modalShouldBeClosed();
    return this;
  }

  public DeliveriesForm showDeliveries(String webhookName) {
    SelenideElement webhook = getWebhook(webhookName);
    webhook.$(".dropdown-toggle").shouldBe(visible).click();
    webhook.$(".js-webhook-deliveries").shouldBe(visible).click();
    modalShouldBeOpen("Recent deliveries for " + webhookName);
    return new DeliveriesForm($(".modal-body"));
  }

  public static class DeliveriesForm {
    private final SelenideElement elt;

    public DeliveriesForm(SelenideElement elt) {
      this.elt = elt;
    }

    public DeliveriesForm countDeliveries(Integer number) {
      this.getDeliveries().shouldHaveSize(number);
      return this;
    }

    public DeliveriesForm isSuccessFull(Integer deliveryIndex) {
      this.getDeliveries().get(deliveryIndex).$(".js-success").should(exist);
      return this;
    }

    public DeliveriesForm payloadContains(Integer deliveryIndex, String... payload) {
      SelenideElement delivery = this.getDeliveries().get(deliveryIndex);
      SelenideElement header = delivery.$(".boxed-group-header").should(exist);
      if (!delivery.$(".boxed-group-inner").exists()) {
        header.click();
      }
      SelenideElement inner = delivery.$(".boxed-group-inner").shouldBe(visible);
      Arrays.stream(payload).forEach(p -> inner.shouldHave(text(p)));
      header.click();
      return this;
    }

    private ElementsCollection getDeliveries() {
      return this.elt.$$(".boxed-group-accordion");
    }
  }

  private static SelenideElement getWebhook(String webhookName) {
    return getWebhooks().find(text(webhookName)).should(exist);
  }

  private static ElementsCollection getWebhooks() {
    return $$(".boxed-group tbody tr");
  }

  private static void modalShouldBeOpen(String title) {
    $(".modal-head").shouldBe(visible).shouldHave(text(title));
  }

  private static void modalShouldBeClosed() {
    $(".modal-head").shouldNot(exist);
  }
}
