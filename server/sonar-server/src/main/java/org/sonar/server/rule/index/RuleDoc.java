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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.debt.DebtCharacteristic;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.RuleParam;
import org.sonar.server.search.BaseDoc;
import org.sonar.server.search.IndexUtils;

/**
 * Implementation of Rule based on an Elasticsearch document
 */
public class RuleDoc extends BaseDoc implements Rule {

  public static final String MANUAL_REPOSITORY = "manual";

  public RuleDoc(Map<String, Object> fields) {
    super(fields);
  }

  @Override
  public RuleKey key() {
    return RuleKey.parse(this.<String>getField(RuleNormalizer.RuleField.KEY.field()));
  }

  /**
   * @deprecated Only use for sqale backward compat. Use key() instead.
   */
  @Deprecated
  public Integer id() {
    return getField(RuleNormalizer.RuleField.ID.field());
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
    return getNullableField(RuleNormalizer.RuleField.INTERNAL_KEY.field());
  }

  @Override
  @CheckForNull
  public String language() {
    return getNullableField(RuleNormalizer.RuleField.LANGUAGE.field());
  }

  @Override
  public String name() {
    return getField(RuleNormalizer.RuleField.NAME.field());
  }

  @Override
  public String htmlDescription() {
    return getNullableField(RuleNormalizer.RuleField.HTML_DESCRIPTION.field());
  }

  @Override
  public String markdownDescription() {
    return getNullableField(RuleNormalizer.RuleField.MARKDOWN_DESCRIPTION.field());
  }

  @Override
  @CheckForNull
  public String effortToFixDescription() {
    return getNullableField(RuleNormalizer.RuleField.FIX_DESCRIPTION.field());
  }

  @Override
  @CheckForNull
  public String severity() {
    return (String) getNullableField(RuleNormalizer.RuleField.SEVERITY.field());
  }

  @Override
  @CheckForNull
  public RuleStatus status() {
    return RuleStatus.valueOf((String) getField(RuleNormalizer.RuleField.STATUS.field()));
  }

  @Override
  public boolean isTemplate() {
    return (Boolean) getField(RuleNormalizer.RuleField.IS_TEMPLATE.field());
  }

  @Override
  @CheckForNull
  public RuleKey templateKey() {
    String templateKey = getNullableField(RuleNormalizer.RuleField.TEMPLATE_KEY.field());
    return templateKey != null ? RuleKey.parse(templateKey) : null;
  }

  @Override
  public List<String> tags() {
    return (List<String>) getField(RuleNormalizer.RuleField.TAGS.field());
  }

  @Override
  public List<String> systemTags() {
    return (List<String>) getField(RuleNormalizer.RuleField.SYSTEM_TAGS.field());
  }

  @Override
  public List<RuleParam> params() {
    List<RuleParam> params = new ArrayList<>();
    final List<Map<String, Object>> esParams = getNullableField(RuleNormalizer.RuleField.PARAMS.field());
    if (esParams != null) {
      for (final Map<String, Object> esParam : esParams) {
        params.add(new RuleParam() {
          @Override
          public String key() {
            return (String) esParam.get(RuleNormalizer.RuleParamField.NAME.field());
          }

          @Override
          public String description() {
            return (String) esParam.get(RuleNormalizer.RuleParamField.DESCRIPTION.field());
          }

          @Override
          public String defaultValue() {
            return (String) esParam.get(RuleNormalizer.RuleParamField.DEFAULT_VALUE.field());
          }

          @Override
          public RuleParamType type() {
            return RuleParamType.parse((String) esParam.get(RuleNormalizer.RuleParamField.TYPE.field()));
          }
        });
      }
    }
    return params;
  }

  @CheckForNull
  @Override
  public RuleParam param(String key) {
    return Iterables.find(params(), new RuleParamMatchKey(key), null);
  }

  @Override
  public boolean debtOverloaded() {
    return BooleanUtils.isTrue(isDebtCharacteristicOverridden()) ||
      BooleanUtils.isTrue(isDebtSubCharacteristicOverridden()) ||
      BooleanUtils.isTrue(isDebtRemediationFunctionOverridden());
  }

  @Override
  @CheckForNull
  public String defaultDebtCharacteristicKey() {
    return (String) getNullableField(RuleNormalizer.RuleField.DEFAULT_CHARACTERISTIC.field());
  }

  @Override
  @CheckForNull
  public String defaultDebtSubCharacteristicKey() {
    return (String) getNullableField(RuleNormalizer.RuleField.DEFAULT_SUB_CHARACTERISTIC.field());
  }

  @Override
  @CheckForNull
  public String debtCharacteristicKey() {
    return (String) getNullableField(RuleNormalizer.RuleField.CHARACTERISTIC.field());
  }

  @Override
  @CheckForNull
  public String debtSubCharacteristicKey() {
    return (String) getNullableField(RuleNormalizer.RuleField.SUB_CHARACTERISTIC.field());
  }

  @CheckForNull
  public Boolean isDebtCharacteristicOverridden() {
    return (Boolean) getNullableField(RuleNormalizer.RuleField.CHARACTERISTIC_OVERLOADED.field());
  }

  @CheckForNull
  public Boolean isDebtSubCharacteristicOverridden() {
    return (Boolean) getNullableField(RuleNormalizer.RuleField.SUB_CHARACTERISTIC_OVERLOADED.field());
  }

  @Override
  @CheckForNull
  public DebtRemediationFunction debtRemediationFunction() {
    if (DebtCharacteristic.NONE.equals(getNullableField(RuleNormalizer.RuleField.SUB_CHARACTERISTIC.field()))) {
      return null;
    }
    final String function = getNullableField(RuleNormalizer.RuleField.DEBT_FUNCTION_TYPE.field());
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
          return (String) getNullableField(RuleNormalizer.RuleField.DEBT_FUNCTION_COEFFICIENT.field());
        }

        @Override
        public String offset() {
          return (String) getNullableField(RuleNormalizer.RuleField.DEBT_FUNCTION_OFFSET.field());
        }
      };
    }
  }

  @Override
  @CheckForNull
  public DebtRemediationFunction defaultDebtRemediationFunction() {
    final String function = getNullableField(RuleNormalizer.RuleField.DEFAULT_DEBT_FUNCTION_TYPE.field());
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
          return (String) getNullableField(RuleNormalizer.RuleField.DEFAULT_DEBT_FUNCTION_COEFFICIENT.field());
        }

        @Override
        public String offset() {
          return (String) getNullableField(RuleNormalizer.RuleField.DEFAULT_DEBT_FUNCTION_OFFSET.field());
        }
      };
    }
  }

  @CheckForNull
  public Boolean isDebtRemediationFunctionOverridden() {
    return (Boolean) getNullableField(RuleNormalizer.RuleField.DEBT_FUNCTION_TYPE_OVERLOADED.field());
  }

  @Override
  @CheckForNull
  public String markdownNote() {
    return getNullableField(RuleNormalizer.RuleField.NOTE.field());
  }

  @Override
  @CheckForNull
  public String noteLogin() {
    return (String) getNullableField(RuleNormalizer.RuleField.NOTE_LOGIN.field());
  }

  @Override
  @CheckForNull
  public Date noteCreatedAt() {
    return IndexUtils.parseDateTime((String) getNullableField(RuleNormalizer.RuleField.NOTE_CREATED_AT.field()));
  }

  @Override
  @CheckForNull
  public Date noteUpdatedAt() {
    return IndexUtils.parseDateTime((String) getNullableField(RuleNormalizer.RuleField.NOTE_UPDATED_AT.field()));
  }

  @Override
  public Date createdAt() {
    return IndexUtils.parseDateTime((String) getNullableField(RuleNormalizer.RuleField.CREATED_AT.field()));
  }

  @Override
  public Date updatedAt() {
    return IndexUtils.parseDateTime((String) getNullableField(RuleNormalizer.RuleField.UPDATED_AT.field()));
  }

  @Override
  public boolean isManual() {
    return getField(RuleNormalizer.RuleField.REPOSITORY.field()).equals(MANUAL_REPOSITORY);
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }

  private static class RuleParamMatchKey implements Predicate<RuleParam> {
    private final String key;

    public RuleParamMatchKey(String key) {
      this.key = key;
    }

    @Override
    public boolean apply(@Nullable RuleParam input) {
      return input != null && input.key().equals(key);
    }
  }
}
