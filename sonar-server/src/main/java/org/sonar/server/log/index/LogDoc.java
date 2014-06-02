package org.sonar.server.log.index;

import org.sonar.core.log.Log;
import org.sonar.core.log.LogDto;
import org.sonar.server.search.BaseDoc;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * @since 4.4
 */
public class LogDoc extends BaseDoc implements Log {

  protected LogDoc(Map<String, Object> fields) {
    super(fields);
  }

  @Override
  public Date time() {
    return this.getField(LogNormalizer.LogFields.TIME.field());
  }

  @Override
  public String author() {
    return this.getField(LogNormalizer.LogFields.AUTHOR.field());
  }

  @Override
  public LogDto.Type type() {
    return LogDto.Type.valueOf((String)this.getField(LogNormalizer.LogFields.TYPE.field()));
  }

  @Override
  public LogDto.Status status() {
    return LogDto.Status.valueOf((String)this.getField(LogNormalizer.LogFields.STATUS.field()));
  }

  @Override
  public Long executionTime() {
    return this.getField(LogNormalizer.LogFields.EXECUTION.field());
  }

  @Override
  public <K extends Serializable> K getPayload() {
   return null;
  }
}
