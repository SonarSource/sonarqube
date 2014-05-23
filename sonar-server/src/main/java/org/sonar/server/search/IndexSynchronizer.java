package org.sonar.server.search;

import org.sonar.server.db.DbClient;

/**
 * @since 4.4
 */
public class IndexSynchronizer {

  private final DbClient db;
  private final IndexClient index;

  public IndexSynchronizer(DbClient db, IndexClient index) {
    this.db = db;
    this.index = index;
  }
}
