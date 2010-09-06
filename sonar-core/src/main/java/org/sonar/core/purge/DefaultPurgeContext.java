package org.sonar.core.purge;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.api.batch.PurgeContext;
import org.sonar.api.database.model.Snapshot;

public class DefaultPurgeContext implements PurgeContext {

  private Integer currentSid;
  private Integer lastSid;

  public DefaultPurgeContext() {
  }

  public DefaultPurgeContext(Snapshot currentSnapshot) {
    this(currentSnapshot, null);
  }

  public DefaultPurgeContext(Snapshot currentSnapshot, Snapshot lastSnapshot) {
    if (currentSnapshot != null) {
      currentSid = currentSnapshot.getId();
    }
    if (lastSnapshot != null) {
      lastSid = lastSnapshot.getId();
    }
  }

  public DefaultPurgeContext(Integer currentSid, Integer lastSid) {
    this.currentSid = currentSid;
    this.lastSid = lastSid;
  }

  public DefaultPurgeContext setLastSnapshotId(Integer lastSid) {
    this.lastSid = lastSid;
    return this;
  }

  public DefaultPurgeContext setCurrentSnapshotId(Integer currentSid) {
    this.currentSid = currentSid;
    return this;
  }

  public Integer getPreviousSnapshotId() {
    return lastSid;
  }

  public Integer getLastSnapshotId() {
    return currentSid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DefaultPurgeContext context = (DefaultPurgeContext) o;

    if (!currentSid.equals(context.currentSid)) {
      return false;
    }
    if (lastSid != null ? !lastSid.equals(context.lastSid) : context.lastSid != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = lastSid != null ? lastSid.hashCode() : 0;
    result = 31 * result + currentSid.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("currentSid", currentSid)
        .append("lastSid", lastSid)
        .toString();
  }
}
