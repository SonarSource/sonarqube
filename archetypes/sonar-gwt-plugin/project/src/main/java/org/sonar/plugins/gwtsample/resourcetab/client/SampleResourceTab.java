package org.sonar.plugins.gwtsample.resourcetab.client;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import org.sonar.gwt.ui.Page;
import org.sonar.wsclient.services.Resource;

public class SampleResourceTab extends Page {

  @Override
  protected Widget doOnResourceLoad(Resource resource) {
    return new Label("This is a sample");
  }
}