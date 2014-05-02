package org.sonar.server.search;

import java.io.Serializable;

/**
 * Created by gamars on 02/05/14.
 * @since
 */
public class EmbeddedIndexAction<K extends Serializable> extends IndexAction {

  private final Object item;
  private final K key;

  public EmbeddedIndexAction(String indexName, Method method, Object item, K key){
    super(indexName, method);
    this.indexName = indexName;
    this.method = method;
    this.key = key;
    this.item = item;
  }

  @Override
  public void doExecute() {
    try {
      if (this.getMethod().equals(Method.DELETE)) {
        index.delete(this.item, this.key);
      } else if (this.getMethod().equals(Method.INSERT)) {
        index.insert(this.item, this.key);
      } else if (this.getMethod().equals(Method.UPDATE)) {
        index.update(this.item, this.key);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Index " + this.getIndexName() + " cannot execute " +
        this.getMethod() + " for " +this.item.getClass().getSimpleName() +
        " on key: "+ this.key, e);
    }
  }
}
