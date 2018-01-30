package org.sonar.server.webhook.ws;

public class WebhookSearchDTO {

  private final String key;
  private final String name;
  private final String url;

  public WebhookSearchDTO(String key, String name, String url) {
    this.key = key;
    this.name = name;
    this.url = url;
  }

  public String getKey() {
    return key;
  }

  public String getName() {
    return name;
  }

  public String getUrl() {
    return url;
  }
}
