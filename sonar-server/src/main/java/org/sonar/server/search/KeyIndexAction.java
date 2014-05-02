package org.sonar.server.search;

import java.io.Serializable;

/**
 * Created by gamars on 02/05/14.
 * @since 4.4
 */
public class KeyIndexAction<K extends Serializable> extends IndexAction {

  private final K key;

  public KeyIndexAction(String indexName, Method method, K key) {
    super(indexName, method);
    this.key = key;
  }

  @Override
  public void doExecute() {
    try {
      if (this.getMethod().equals(Method.DELETE)) {
        index.deleteByKey(this.key);
      } else if (this.getMethod().equals(Method.INSERT)) {
        index.insertByKey(this.key);
      } else if (this.getMethod().equals(Method.UPDATE)) {
        index.updateByKey(this.key);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Index " + this.getIndexName() + " cannot execute " +
        this.getMethod() + " for " + key);
    }
  }
}
