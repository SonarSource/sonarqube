package com.mycompany.sonar.gwt.page.client;

import com.google.gwt.core.client.GWT;

public interface I18nConstants extends com.google.gwt.i18n.client.Constants {

  static I18nConstants INSTANCE = GWT.create(I18nConstants.class);

  @DefaultStringValue("This is a sample")
  String sample();
}
