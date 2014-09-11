package org.sonar.server.db.fake;

import org.sonar.core.persistence.Dto;

public class FakeDto extends Dto<String> {

  private long id;
  private String key;

  @Override
  public String getKey() {
    return key;
  }

  public long getId() {
    return id;
  }

  public FakeDto setId(long id) {
    this.id = id;
    return this;
  }

  public FakeDto setKey(String key) {
    this.key = key;
    return this;
  }
}
