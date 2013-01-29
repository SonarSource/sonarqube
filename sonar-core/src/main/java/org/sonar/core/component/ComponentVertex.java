package org.sonar.core.component;

import org.sonar.api.component.Component;
import org.sonar.core.graph.BeanVertex;

public class ComponentVertex extends BeanVertex implements Component {

  public String key() {
    return (String) getProperty("key");
  }

  public String name() {
    return (String) getProperty("name");
  }

  public String qualifier() {
    return (String) getProperty("qualifier");
  }

  void copyFrom(Component component) {
    setProperty("key", component.key());
    setProperty("name", component.name());
    setProperty("qualifier", component.qualifier());
    if (component instanceof ResourceComponent) {
      setProperty("sid", ((ResourceComponent) component).snapshotId());
    }
  }
}
