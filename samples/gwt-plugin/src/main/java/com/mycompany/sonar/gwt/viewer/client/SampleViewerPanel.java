package com.mycompany.sonar.gwt.viewer.client;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import org.sonar.gwt.ui.Page;
import org.sonar.wsclient.services.Resource;

public class SampleViewerPanel extends Page {

  @Override
  protected Widget doOnResourceLoad(Resource resource) {
    return new Label("This is a sample");
  }
}