package org.sonar.core.component;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import org.sonar.api.component.Perspective;
import org.sonar.core.graph.BeanVertex;
import org.sonar.core.graph.GraphUtil;

public abstract class GraphPerspectiveLoader<T extends Perspective> {

  protected final String perspectiveKey;
  protected final Class<T> perspectiveClass;

  protected GraphPerspectiveLoader(String perspectiveKey, Class<T> perspectiveClass) {
    this.perspectiveKey = perspectiveKey;
    this.perspectiveClass = perspectiveClass;
  }

  public T load(ComponentVertex component) {
    Vertex perspectiveVertex = GraphUtil.singleAdjacent(component.element(), Direction.OUT, getPerspectiveKey());
    if (perspectiveVertex != null) {
      return (T) component.beanGraph().wrap(perspectiveVertex, getBeanClass());
    }
    return null;
  }

  protected String getPerspectiveKey() {
    return perspectiveKey;
  }

  protected Class<T> getPerspectiveClass() {
    return perspectiveClass;
  }

  protected abstract Class<? extends BeanVertex> getBeanClass();
}
