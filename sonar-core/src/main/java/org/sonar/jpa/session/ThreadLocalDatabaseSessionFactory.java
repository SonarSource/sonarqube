package org.sonar.jpa.session;

import org.sonar.api.database.DatabaseSession;

public class ThreadLocalDatabaseSessionFactory implements DatabaseSessionFactory {

  private final ThreadLocal<JpaDatabaseSession> threadSession = new ThreadLocal<JpaDatabaseSession>();
  private final DatabaseConnector connector;

  public ThreadLocalDatabaseSessionFactory(DatabaseConnector connector) {
    this.connector = connector;
  }

  public DatabaseSession getSession() {
    JpaDatabaseSession session = threadSession.get();
    if (session == null) {
      session = new JpaDatabaseSession(connector);
      session.start();
      threadSession.set(session);
    }
    return session;
  }

  public void clear() {
    JpaDatabaseSession session = threadSession.get();
    if (session != null) {
      session.stop();
    }
    threadSession.set(null);
  }

  public void stop() {
    clear();
  }
}
