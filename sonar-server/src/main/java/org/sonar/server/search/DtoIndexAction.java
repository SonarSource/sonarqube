package org.sonar.server.search;

import org.sonar.core.db.Dto;

/**
 * Created by gamars on 02/05/14.
 * @since 4.4
 */
public class DtoIndexAction<E extends Dto> extends IndexAction {

  private final E item;

  public DtoIndexAction(String indexName, Method method, E item) {
    super(indexName, method);
    this.item = item;
  }

  @Override
  public void doExecute() {
    try {
      if (this.getMethod().equals(Method.DELETE)) {
        index.deleteByDto(this.item);
      } else if (this.getMethod().equals(Method.INSERT)) {
        index.insertByDto(this.item);
      } else if (this.getMethod().equals(Method.UPDATE)) {
        index.updateByDto(this.item);
      }
    } catch (Exception e) {
      e.printStackTrace();

      throw new IllegalStateException("Index " + this.getIndexName() + " cannot execute " +
        this.getMethod() + " for " + item.getKey());

    }
  }

  @Override
  public String toString() {
    return "{DtoIndexItem {key: " + item.getKey() + "}";
  }
}

