package org.sonar.server.db.fake;

import org.sonar.api.utils.System2;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.BaseDao;

public class FakeDao extends BaseDao<FakeMapper, FakeDto, String> {

  public FakeDao(System2 system2) {
    super(FakeMapper.class, system2);
  }

  @Override
  protected FakeDto doInsert(DbSession session, FakeDto item) {
    mapper(session).insert(item);
    return item;
  }

  @Override
  protected FakeDto doGetNullableByKey(DbSession session, String key) {
    return mapper(session).selectByKey(key);
  }
}
