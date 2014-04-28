package org.sonar.server.rule2;

import java.util.Collection;

import org.sonar.core.persistence.MyBatis;
import org.sonar.server.cluster.WorkQueue;
import org.sonar.api.rule.RuleKey;
import org.sonar.server.db.BaseDao;

public class RuleDao extends BaseDao<RuleDto, RuleKey> {

  protected RuleDao(WorkQueue workQueue, MyBatis myBatis) {
    super(workQueue, myBatis);
  }

  @Override
  protected String getIndexName() {
    return RuleConstants.INDEX_NAME;
  }

  @Override
  public Collection<RuleKey> insertsSince(Long timestamp) {
    // TODO Auto-generated method stub
    return null;
  }
}
