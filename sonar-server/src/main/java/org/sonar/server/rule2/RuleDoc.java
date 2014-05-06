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
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.server.rule2.RuleNormalizer.RuleField;

import javax.annotation.CheckForNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Implementation of Rule based on an Elasticsearch document
 */
class RuleDoc implements Rule {

  private final Map<String, Object> fields;

  public RuleDoc(Map<String, Object> fields) {
    this.fields = fields;
  }

  @Override
  public RuleKey key() {
    String repo = (String) fields.get(RuleField.REPOSITORY.key());
    String key = (String) fields.get(RuleField.KEY.key());
    if(repo == null || key == null
      || repo.isEmpty() || key.isEmpty()){
      throw new IllegalStateException("Missing values for RuleKey in RuleDoc");
    } else {
      return RuleKey.of(repo, key);
    }
  }

  @Override
  @CheckForNull
  public String internalKey() {
    return (String) fields.get(RuleField.INTERNAL_KEY.key());
  }

  @Override
  @CheckForNull
  public String language() {
    return (String) fields.get(RuleField.LANGUAGE.key());
  }

  @Override
  @CheckForNull
  public String name() {
    return (String) fields.get(RuleField.NAME.key());
  }

  @Override
  @CheckForNull
  public String htmlDescription() {
    return (String) fields.get(RuleField.HTML_DESCRIPTION.key());
  }

  @Override
  @CheckForNull
  public String severity() {
    return (String) fields.get(RuleField.SEVERITY.key());
  }

  @Override
  @CheckForNull
  public RuleStatus status() {
    return RuleStatus.valueOf((String) fields.get(RuleField.STATUS.key()));
  }

  @Override
  @CheckForNull
  public boolean template() {
    return (Boolean) fields.get(RuleField.TEMPLATE.key());
  }

  @Override
  @CheckForNull
  public List<String> tags() {
    return (List<String>) fields.get(RuleField.TAGS.key());
  }

  @Override
  @CheckForNull
  public List<String> systemTags() {
    return (List<String>) fields.get(RuleField.SYSTEM_TAGS.key());
  }

  @Override
  @CheckForNull
  public List<RuleParam> params() {
    List<RuleParam> params = new ArrayList<RuleParam>();
    if (this.fields.get(RuleField.PARAMS.key()) != null) {
      Map<String, Map<String, Object>> esParams = (Map<String, Map<String, Object>>) this.fields.get(RuleField.PARAMS.key());
      for (final Map<String, Object> param : esParams.values()) {
        params.add(new RuleParam() {
          {
            this.fields = param;
          }

          Map<String, Object> fields;

          @Override
          public String key() {
            return (String) param.get(RuleNormalizer.RuleParamField.NAME.key());
          }

          @Override
          public String description() {
            return (String) param.get(RuleNormalizer.RuleParamField.DESCRIPTION.key());
          }

          @Override
          public String defaultValue() {
            return (String) param.get(RuleNormalizer.RuleParamField.DEFAULT_VALUE.key());
          }

          @Override
          public RuleParamType type() {
            return RuleParamType
              .parse((String) param.get(RuleNormalizer.RuleParamField.TYPE.key()));
          }
        });
      }
    }
    return params;
  }

  @Override
  @CheckForNull
  public String debtCharacteristicKey() {
    throw new UnsupportedOperationException("TODO");
  }

  @Override
  @CheckForNull
  public String debtSubCharacteristicKey() {
    throw new UnsupportedOperationException("TODO");
  }

  @Override
  @CheckForNull
  public DebtRemediationFunction debtRemediationFunction() {
    throw new UnsupportedOperationException("TODO");
  }

  @Override
  @CheckForNull
  public Date createdAt() {
    return (Date) fields.get(RuleField.CREATED_AT.key());
  }

  @Override
  @CheckForNull
  public Date updatedAt() {
    return (Date) fields.get(RuleField.UPDATED_AT.key());
  }
}
