package org.sonar.server.log.index;

import org.sonar.core.log.Log;

import java.util.Collection;
import java.util.Date;

/**
 * @since 4.4
 */
public class LogQuery {

  private Date since;
  private Date to;
  private Collection<Log.Type> types;

  public LogQuery() {
  }

  public Date getSince() {
    return since;
  }

  public void setSince(Date since) {
    this.since = since;
  }

  public Date getTo() {
    return to;
  }

  public void setTo(Date to) {
    this.to = to;
  }

  public Collection<Log.Type> getTypes() {
    return types;
  }

  public void setTypes(Collection<Log.Type> types) {
    this.types = types;
  }
}
