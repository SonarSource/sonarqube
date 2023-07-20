package org.sonar.db.issue;

import java.util.List;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class AnticipatedTransitionDao implements Dao {

  public void insert(DbSession session, AnticipatedTransitionDto transition) {
    mapper(session).insert(transition);
  }

  public void delete(DbSession session, String uuid) {
    mapper(session).delete(uuid);
  }

  public List<AnticipatedTransitionDto> selectByProjectUuid(DbSession session, String projectUuid) {
    return mapper(session).selectByProjectUuid(projectUuid);
  }

  private static AnticipatedTransitionMapper mapper(DbSession session) {
    return session.getMapper(AnticipatedTransitionMapper.class);
  }
}
