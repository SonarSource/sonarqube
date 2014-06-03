/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.log.db;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.core.log.Activity;
import org.sonar.core.persistence.Dto;

import java.util.Date;

/**
 * @since 4.4
 */
public final class LogDto extends Dto<LogKey> {


  public static enum Payload {
    TEST_ACTIVITY("org.sonar.server.log.db.TestActivity"),
    ACTIVE_RULE_CHANGE("org.sonar.server.qualityprofile.ActiveRuleChange");

    private final String clazz;

    private Payload(String ActiveClass) {
      clazz = ActiveClass;
    }

    public static Payload forClassName(String className){
      for(Payload payload:Payload.values()){
        if(payload.clazz.equals(className))
          return payload;
      }
      return null;
    }
  }

  private Date time;
  private Payload payload;
  private String author;

  private Long executionTime;
  private String data;

  protected LogDto(){

  }

  public LogDto(String user, Activity activity) {
    this.time = new Date();
    this.author = user;
    this.payload = Payload.forClassName(activity.getClass().getCanonicalName());
    this.data = activity.serialize();
  }

  @Override
  public LogKey getKey() {
    return LogKey.of(time, payload, author);
  }

  public Date getTime() {
    return time;
  }

  public Long getExecutionTime() {
    return executionTime;
  }

  public LogDto setExecutionTime(Long executionTime) {
    this.executionTime = executionTime;
    return this;
  }

  public String getAuthor() {
    return author;
  }

  public <K extends Activity> K getActivity() {
    try {
      Activity activity = ((Activity) Class.forName(this.payload.clazz, true, ClassLoader.getSystemClassLoader()).newInstance());
      return (K) activity.deSerialize(this.data);
    } catch (Exception e) {
      throw new IllegalStateException("Could not read Activity from DB. '" + this.payload
        + "' is most likely missing a public no-args ctor", e);
    }
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
  }
}
