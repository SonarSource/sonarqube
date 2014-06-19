package org.sonar.server.qualityprofile;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.sonar.core.activity.Activity;
import org.sonar.server.activity.index.ActivityQuery;

import java.util.Collection;

/**
 * @since 4.4
 */
public class QProfileActivityQuery extends ActivityQuery {


  Collection<String> qprofileKeys;

  public QProfileActivityQuery() {
    super();
    this.setTypes(ImmutableSet.of(Activity.Type.QPROFILE));
    qprofileKeys = Lists.newArrayList();
  }

  public Collection<String> getQprofileKeys() {
    return qprofileKeys;
  }

  public void setQprofileKeys(Collection<String> qprofileKeys) {
    this.qprofileKeys = qprofileKeys;
  }
}
