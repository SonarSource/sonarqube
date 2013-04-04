package org.sonar.core.test;

import org.sonar.api.test.MutableTestable;
import org.sonar.core.component.GraphPerspectiveLoader;
import org.sonar.core.graph.BeanVertex;

public class TestablePerspectiveLoader extends GraphPerspectiveLoader<MutableTestable> {

  static final String PERSPECTIVE_KEY = "testable";

  public TestablePerspectiveLoader() {
    super(PERSPECTIVE_KEY, MutableTestable.class);
  }

  @Override
  protected Class<? extends BeanVertex> getBeanClass() {
    return DefaultTestable.class;
  }
}
