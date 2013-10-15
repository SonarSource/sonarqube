package org.sonar.wsclient.system.internal;

import org.sonar.wsclient.system.Migration;
import org.sonar.wsclient.unmarshallers.JsonUtils;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Map;

public class DefaultMigration implements Migration {

  private final Map json;

  public DefaultMigration(Map json) {
    this.json = json;
  }

  @Override
  public boolean operationalWebapp() {
    return JsonUtils.getBoolean(json, "operational");
  }

  @Override
  public Status status() {
    return Status.valueOf(JsonUtils.getString(json, "state"));
  }

  @Override
  public String message() {
    return JsonUtils.getString(json, "message");
  }

  @Override
  @Nullable
  public Date startedAt() {
    return JsonUtils.getDateTime(json, "startedAt");
  }
}
