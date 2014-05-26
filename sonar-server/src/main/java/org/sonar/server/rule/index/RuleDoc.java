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
    return RuleKey.parse(this.<String>getField(RuleNormalizer.RuleField.KEY.key()));
  }

  /**
   * Alias for backward-compatibility with SQALE
   */
  public RuleKey ruleKey() {
    return key();
  }

  @Override
  public String internalKey() {
    return getNullableField(RuleNormalizer.RuleField.INTERNAL_KEY.key());
  }

  @Override
  public String language() {
    return getField(RuleNormalizer.RuleField.LANGUAGE.key());
  }

  @Override
  public String name() {
    return getField(RuleNormalizer.RuleField.NAME.key());
  }

  @Override
  public String htmlDescription() {
    return getNullableField(RuleNormalizer.RuleField.HTML_DESCRIPTION.key());
  }

  @Override
  public String severity() {
    return (String) getField(RuleNormalizer.RuleField.SEVERITY.key());
  }

  @Override
  public RuleStatus status() {
    return RuleStatus.valueOf((String) getField(RuleNormalizer.RuleField.STATUS.key()));
  }

  @Override
  public boolean template() {
    return (Boolean) getField(RuleNormalizer.RuleField.TEMPLATE.key());
  }

  @Override
  public List<String> tags() {
    return (List<String>) getField(RuleNormalizer.RuleField.TAGS.key());
  }

  @Override
  public List<String> systemTags() {
    return (List<String>) getField(RuleNormalizer.RuleField.SYSTEM_TAGS.key());
  }

  @Override
  public List<RuleParam> params() {
    List<RuleParam> params = new ArrayList<RuleParam>();
    List<Map<String, Object>> esParams = getNullableField(RuleNormalizer.RuleField.PARAMS.key());
    if (esParams != null) {
      for (final Map<String, Object> esParam : esParams) {
        params.add(new RuleParam() {
          {
            this.fields = esParam;
          }

          Map<String, Object> fields;

          @Override
          public String key() {
            return (String) esParam.get(RuleNormalizer.RuleParamField.NAME.key());
          }

          @Override
          public String description() {
            return (String) esParam.get(RuleNormalizer.RuleParamField.DESCRIPTION.key());
          }

          @Override
          public String defaultValue() {
            return (String) esParam.get(RuleNormalizer.RuleParamField.DEFAULT_VALUE.key());
          }

          @Override
          public RuleParamType type() {
            return RuleParamType
              .parse((String) esParam.get(RuleNormalizer.RuleParamField.TYPE.key()));
          }
        });
      }
    }
    return params;
  }

  @Override
  public String debtCharacteristicKey() {
    return (String) getNullableField(RuleNormalizer.RuleField.CHARACTERISTIC.key());
  }

  @Override
  public String debtSubCharacteristicKey() {
    return (String) getNullableField(RuleNormalizer.RuleField.SUB_CHARACTERISTIC.key());
  }

  @Override
  public DebtRemediationFunction debtRemediationFunction() {
    final String function = getNullableField(RuleNormalizer.RuleField.DEBT_FUNCTION_TYPE.key());
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
          return (String) getNullableField(RuleNormalizer.RuleField.DEBT_FUNCTION_COEFFICIENT.key());
        }

        @Override
        public String offset() {
          return (String) getNullableField(RuleNormalizer.RuleField.DEBT_FUNCTION_OFFSET.key());
        }
      };
    }
  }

  @Override
  public String markdownNote() {
    return getNullableField(RuleNormalizer.RuleField.NOTE.key());
  }

  @Override
  public String noteLogin() {
    return (String) getNullableField(RuleNormalizer.RuleField.NOTE_LOGIN.key());
  }

  @Override
  public Date noteCreatedAt() {
    return IndexUtils.parseDateTime((String) getNullableField(RuleNormalizer.RuleField.NOTE_CREATED_AT.key()));
  }

  @Override
  public Date noteUpdatedAt() {
    return IndexUtils.parseDateTime((String) getNullableField(RuleNormalizer.RuleField.NOTE_UPDATED_AT.key()));
  }

  @Override
  public Date createdAt() {
    return IndexUtils.parseDateTime((String) getNullableField(RuleNormalizer.RuleField.CREATED_AT.key()));
  }

  @Override
  public Date updatedAt() {
    return IndexUtils.parseDateTime((String) getNullableField(RuleNormalizer.RuleField.UPDATED_AT.key()));
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }
}
