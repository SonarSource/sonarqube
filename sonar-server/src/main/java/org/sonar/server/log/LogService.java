package org.sonar.server.log;

import org.sonar.core.log.Activity;
import org.sonar.core.log.db.LogDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.user.UserSession;

/**
 * Log service is used to log Activity classes which represents an event to DB and Index.
 *
 * @see org.sonar.core.log.Activity
 * @since 4.4
 */
public class LogService {

  private final DbClient dbClient;

  public LogService(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public void log(Activity activity) {
    DbSession session = dbClient.openSession(false);
    try {
      this.log(session, activity);
    } finally {
      session.close();
    }
  }

  public void log(DbSession session, Activity activity) {
    dbClient.logDao().insert(session, new LogDto(UserSession.get().login(), activity));
  }
}
