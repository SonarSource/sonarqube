package com.mycompany.sonar.gwt.page.client;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.sonar.gwt.ui.Page;
import org.sonar.wsclient.services.Resource;

public class SamplePagePanel extends Page {

  @Override
  protected Widget doOnResourceLoad(Resource resource) {
    VerticalPanel panel = new VerticalPanel();
    panel.add(new Label(resource.getName(true)));
    panel.add(new Label(I18nConstants.INSTANCE.sample()));
    return panel;
  }
}
