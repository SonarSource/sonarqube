package com.mycompany.sonar.gwt;

import com.mycompany.sonar.gwt.page.SamplePage;
import com.mycompany.sonar.gwt.viewer.SampleViewer;
import org.sonar.api.SonarPlugin;

import java.util.Arrays;
import java.util.List;

public final class GwtPlugin extends SonarPlugin {

  public List getExtensions() {
    return Arrays.asList(SampleViewer.class, SamplePage.class);
  }

  public String toString() {
    return getKey();
  }
}