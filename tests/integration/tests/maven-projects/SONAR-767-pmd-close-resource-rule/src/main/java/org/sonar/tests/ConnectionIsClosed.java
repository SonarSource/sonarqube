package org.sonar.tests;

import java.sql.Connection;

public class ConnectionIsClosed {

  public void connectionIsClosed() throws Exception {
    Connection c = getConnection();
    try {
      // do stuff
      c.commit();

    } finally {
      c.close();
    }
  }

  protected Connection getConnection() {
    return null;
  }
}
