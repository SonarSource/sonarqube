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
package org.sonar.server.rule.index;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.RuleParam;
import org.sonar.server.search.BaseDoc;
import org.sonar.server.search.IndexUtils;

import javax.annotation.CheckForNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Implementation of Rule based on an Elasticsearch document
 */
public class RuleDoc extends BaseDoc implements Rule {

  RuleDoc(Map<String, Object> fields) {
    super(fields);
  }

  @Override
  public RuleKey key() {
    String key = getField(RuleNormalizer.RuleField.KEY.key());
    if (key == null || key.isEmpty()) {
      throw new IllegalStateException("Missing values for RuleKey in RuleDoc");
    } else {
      return RuleKey.parse(key);
    }
  }

  /**
   * Alias for backward-compatibility with SQALE
   */
  public RuleKey ruleKey() {
    return key();
  }

  @Override
  @CheckForNull
  public String internalKey() {
    return getField(RuleNormalizer.RuleField.INTERNAL_KEY.key());
  }

  @Override
  public String markdownNote() {
    return getField(RuleNormalizer.RuleField.NOTE.key());
  }

  @Override
  @CheckForNull
  public String language() {
    return getField(RuleNormalizer.RuleField.LANGUAGE.key());
  }

  @Override
  @CheckForNull
  public String name() {
    return getField(RuleNormalizer.RuleField.NAME.key());
  }

  @Override
  @CheckForNull
  public String htmlDescription() {
    return getField(RuleNormalizer.RuleField.HTML_DESCRIPTION.key());
  }

  @Override
  @CheckForNull
  public String severity() {
    return (String) getField(RuleNormalizer.RuleField.SEVERITY.key());
  }

  @Override
  @CheckForNull
  public RuleStatus status() {
    return RuleStatus.valueOf((String) getField(RuleNormalizer.RuleField.STATUS.key()));
  }

  @Override
  @CheckForNull
  public boolean template() {
    return (Boolean) getField(RuleNormalizer.RuleField.TEMPLATE.key());
  }

  @Override
  @CheckForNull
  public List<String> tags() {
    return (List<String>) getField(RuleNormalizer.RuleField.TAGS.key());
  }

  @Override
  @CheckForNull
  public List<String> systemTags() {
    return (List<String>) getField(RuleNormalizer.RuleField.SYSTEM_TAGS.key());
  }

  @Override
  @CheckForNull
  public List<RuleParam> params() {
    List<RuleParam> params = new ArrayList<RuleParam>();
    if (this.getField(RuleNormalizer.RuleField.PARAMS.key()) != null) {
      List<Map<String, Object>> esParams = this.getField(RuleNormalizer.RuleField.PARAMS.key());
      for (final Map<String, Object> param : esParams) {
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
    return (String) getField(RuleNormalizer.RuleField.CHARACTERISTIC.key());
  }

  @Override
  @CheckForNull
  public String debtSubCharacteristicKey() {
    return (String) getField(RuleNormalizer.RuleField.SUB_CHARACTERISTIC.key());
  }

  @Override
  @CheckForNull
  public DebtRemediationFunction debtRemediationFunction() {
    final String function = this.getField(RuleNormalizer.RuleField.DEBT_FUNCTION_TYPE.key());
    if (function == null || function.isEmpty()) {
      return null;
    } else {
      return new DebtRemediationFunction() {
        @Override
        public Type type() {
          return Type.valueOf(function.toUpperCase());
        }

        @Override
        public String coefficient() {
          return (String) getField(RuleNormalizer.RuleField.DEBT_FUNCTION_COEFFICIENT.key());
        }

        @Override
        public String offset() {
          return (String) getField(RuleNormalizer.RuleField.DEBT_FUNCTION_OFFSET.key());
        }
      };
    }
  }

  @Override
  @CheckForNull
  public String noteLogin() {
    return (String) getField(RuleNormalizer.RuleField.NOTE_LOGIN.key());
  }

  @Override
  public Date noteCreatedAt() {
    return IndexUtils.parseDateTime((String) getField(RuleNormalizer.RuleField.NOTE_CREATED_AT.key()));
  }

  @Override
  public Date noteUpdatedAt() {
    return IndexUtils.parseDateTime((String) getField(RuleNormalizer.RuleField.NOTE_UPDATED_AT.key()));
  }

  @Override
  @CheckForNull
  public Date createdAt() {
    return IndexUtils.parseDateTime((String) getField(RuleNormalizer.RuleField.CREATED_AT.key()));
  }

  @Override
  @CheckForNull
  public Date updatedAt() {
    return IndexUtils.parseDateTime((String) getField(RuleNormalizer.RuleField.UPDATED_AT.key()));
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }
}
