package com.mycompany.sonar.gwt.viewer.client;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import org.sonar.gwt.ui.Page;
import org.sonar.wsclient.gwt.AbstractCallback;
import org.sonar.wsclient.gwt.Sonar;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

public class SampleViewerPanel extends Page {

  private Label label;

  @Override
  protected Widget doOnResourceLoad(Resource resource) {
    label = new Label("Loading value");
    loadMeasureFromServer(resource);
    return label;
  }

  // Ajax call to web service
  private void loadMeasureFromServer(Resource resource) {
    ResourceQuery query = ResourceQuery.createForResource(resource, "random");
    Sonar.getInstance().find(query, new AbstractCallback<Resource>() {
      @Override
      protected void doOnResponse(Resource result) {
        Measure measure = result.getMeasure("random");
        if (measure==null || measure.getValue()==null) {
          label.setText("No random value");
        } else {
          label.setText("Random value inserted during analysis: " + measure.getValue());
        }
      }
    });
  }
}