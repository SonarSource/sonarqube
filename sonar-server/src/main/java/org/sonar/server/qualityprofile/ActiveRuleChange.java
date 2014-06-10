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
package org.sonar.server.qualityprofile;

import com.google.common.collect.Maps;
import org.sonar.core.log.Loggable;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Map;

public class ActiveRuleChange implements Loggable {

  static enum Type {
    ACTIVATED, DEACTIVATED, UPDATED
  }

  private final Type type;
  private final ActiveRuleKey key;
  private boolean inheritedChange = false;
  private String previousSeverity = null, severity = null;
  private ActiveRule.Inheritance previousInheritance = null, inheritance = null;
  private Map<String, String> parameters = Maps.newHashMap();

  public ActiveRuleChange() {
    type = null;
    key = null;
  }

  ActiveRuleChange(Type type, ActiveRuleKey key) {
    this.type = type;
    this.key = key;
  }

  public ActiveRuleKey getKey() {
    return key;
  }

  public Type getType() {
    return type;
  }

  public boolean isInheritedChange() {
    return inheritedChange;
  }

  public void setInheritedChange(boolean b) {
    this.inheritedChange = b;
  }

  @CheckForNull
  public String getPreviousSeverity() {
    return previousSeverity;
  }

  public void setPreviousSeverity(@Nullable String s) {
    this.previousSeverity = s;
  }

  @CheckForNull
  public String getSeverity() {
    return severity;
  }

  public ActiveRuleChange setSeverity(@Nullable String severity) {
    this.severity = severity;
    return this;
  }

  public ActiveRuleChange setInheritance(@Nullable ActiveRule.Inheritance inheritance) {
    this.inheritance = inheritance;
    return this;
  }

  @CheckForNull
  public ActiveRule.Inheritance getInheritance() {
    return this.inheritance;
  }

  @CheckForNull
  public Map<String, String> getParameters() {
    return parameters;
  }

  public void setParameter(String key, @Nullable String value) {
    parameters.put(key, value);
  }

  @Override
  public Map<String, String> getDetails() {
    return null;
  }

  @Override
  public Long getExecutionTime() {
    return null;
  }
}
