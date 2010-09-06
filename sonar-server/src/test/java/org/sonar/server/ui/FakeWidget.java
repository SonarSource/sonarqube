package org.sonar.server.ui;

import org.sonar.api.web.Widget;

public class FakeWidget implements Widget {

  private String id;
  private String title;

  public FakeWidget(String id, String title) {
    this.id = id;
    this.title = title;
  }

  public FakeWidget() {
    this("fake-widget", "fake widget");
  }

  public String getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }
}