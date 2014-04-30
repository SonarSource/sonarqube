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
package org.sonar.server.rule2;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.server.search.Hit;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RuleImpl implements Rule {

  private Map<String, Object> fields;

  public RuleImpl(){
    this.fields = new HashMap<String, Object>();
  }

  private RuleImpl(Map<String, Object> fields){
    this.fields = fields;
  }

  @Override
  public RuleKey key() {
    return RuleKey.of((String)this.fields.get("repositoryKey"),
      (String)this.fields.get("ruleKey"));
  }

  @Override
  public String language() {
    return (String) this.fields.get("language");
  }

  @Override
  public String name() {
    return (String) this.fields.get("name");
  }

  @Override
  public String description() {
    return (String) this.fields.get("description");
  }

  @Override
  public String severity() {
    return (String) this.fields.get("severity");
  }

  @Override
  public RuleStatus status() {
    return RuleStatus.valueOf( (String) this.fields.get("status"));
  }

  @Override
  public boolean template() {
    //FIXME missign information in map.
    return false;
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<String> tags() {
    return (List<String>) this.fields.get("tags");
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<String> systemTags() {
    return (List<String>) this.fields.get("systemTags");
  }

  @Override
  public List<RuleParam> params() {
    //FIXME not yet Implemented in ES
    return Collections.emptyList();
  }

  @Override
  public String debtCharacteristicKey() {
    return (String) this.fields.get("debtCharacteristicKey");
  }

  @Override
  public String debtSubCharacteristicKey() {
    return (String) this.fields.get("debtSubCharacteristicKey");
  }

  @Override
  public DebtRemediationFunction debtRemediationFunction() {
    //FIXME how to construct from string "defaultRemediationFunction"
    return null;
  }

  @Override
  public Date createdAt() {
    return (Date) this.fields.get("createdAt");
  }

  @Override
  public Date updatedAt() {
    return (Date) this.fields.get("updatedAt");
  }

  public static Rule fromHit(Hit hit) {
    return new RuleImpl(hit.getFields());
  }
}
