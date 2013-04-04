package org.sonar.core.test;

import org.sonar.api.test.MutableTestPlan;
import org.sonar.core.component.GraphPerspectiveLoader;
import org.sonar.core.graph.BeanVertex;

public class TestPlanPerspectiveLoader extends GraphPerspectiveLoader<MutableTestPlan> {

  static final String PERSPECTIVE_KEY = "testplan";

  public TestPlanPerspectiveLoader() {
    super(PERSPECTIVE_KEY, MutableTestPlan.class);
  }

  @Override
  protected Class<? extends BeanVertex> getBeanClass() {
    return DefaultTestPlan.class;
  }
}
