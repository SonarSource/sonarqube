package org.sonar.tests;

import java.sql.Connection;

public class ConnectionIsNotClosed {
  public void connectionIsNotClosed() throws Exception {
    Connection c = getConnection();
    try {
      // do stuff
      c.commit();

    } finally {
      // c.close();
    }
  }

  protected Connection getConnection() {
    return null;
  }
}
