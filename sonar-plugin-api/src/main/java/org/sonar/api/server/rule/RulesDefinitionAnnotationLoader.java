/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.server.rule;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import java.lang.reflect.Field;
import java.util.List;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.api.utils.FieldUtils2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.check.Cardinality;

/**
 * Read definitions of rules based on the annotations provided by sonar-check-api. It is used
 * to feed {@link RulesDefinition}.
 *
 * @see org.sonar.api.server.rule.RulesDefinition
 * @since 4.3
 */
public class RulesDefinitionAnnotationLoader {

  private static final Logger LOG = Loggers.get(RulesDefinitionAnnotationLoader.class);

  private static final Function<Class<?>, RuleParamType> TYPE_FOR_CLASS = Functions.forMap(
    ImmutableMap.<Class<?>, RuleParamType>builder()
      .put(Integer.class, RuleParamType.INTEGER)
      .put(int.class, RuleParamType.INTEGER)
      .put(Float.class, RuleParamType.FLOAT)
      .put(float.class, RuleParamType.FLOAT)
      .put(Boolean.class, RuleParamType.BOOLEAN)
      .put(boolean.class, RuleParamType.BOOLEAN)
      .build(),
    RuleParamType.STRING
  );

  public void load(RulesDefinition.NewExtendedRepository repo, Class... annotatedClasses) {
    for (Class annotatedClass : annotatedClasses) {
      loadRule(repo, annotatedClass);
    }
  }

  @CheckForNull
  RulesDefinition.NewRule loadRule(RulesDefinition.NewExtendedRepository repo, Class clazz) {
    org.sonar.check.Rule ruleAnnotation = AnnotationUtils.getAnnotation(clazz, org.sonar.check.Rule.class);
    if (ruleAnnotation != null) {
      return loadRule(repo, clazz, ruleAnnotation);
    } else {
      LOG.warn("The class " + clazz.getCanonicalName() + " should be annotated with " + org.sonar.check.Rule.class);
      return null;
    }
  }

  private static RulesDefinition.NewRule loadRule(RulesDefinition.NewExtendedRepository repo, Class clazz, org.sonar.check.Rule ruleAnnotation) {
    String ruleKey = StringUtils.defaultIfEmpty(ruleAnnotation.key(), clazz.getCanonicalName());
    String ruleName = StringUtils.defaultIfEmpty(ruleAnnotation.name(), null);
    String description = StringUtils.defaultIfEmpty(ruleAnnotation.description(), null);

    RulesDefinition.NewRule rule = repo.createRule(ruleKey);
    rule.setName(ruleName).setHtmlDescription(description);
    rule.setSeverity(ruleAnnotation.priority().name());
    rule.setTemplate(ruleAnnotation.cardinality() == Cardinality.MULTIPLE);
    rule.setStatus(RuleStatus.valueOf(ruleAnnotation.status()));
    rule.setTags(ruleAnnotation.tags());

    List<Field> fields = FieldUtils2.getFields(clazz, true);
    for (Field field : fields) {
      loadParameters(rule, field);
    }

    return rule;
  }

  private static void loadParameters(RulesDefinition.NewRule rule, Field field) {
    org.sonar.check.RuleProperty propertyAnnotation = field.getAnnotation(org.sonar.check.RuleProperty.class);
    if (propertyAnnotation != null) {
      String fieldKey = StringUtils.defaultIfEmpty(propertyAnnotation.key(), field.getName());
      RulesDefinition.NewParam param = rule.createParam(fieldKey)
        .setDescription(propertyAnnotation.description())
        .setDefaultValue(propertyAnnotation.defaultValue());

      if (!StringUtils.isBlank(propertyAnnotation.type())) {
        try {
          param.setType(RuleParamType.parse(propertyAnnotation.type().trim()));
        } catch (IllegalArgumentException e) {
          throw new IllegalArgumentException("Invalid property type [" + propertyAnnotation.type() + "]", e);
        }
      } else {
        param.setType(guessType(field.getType()));
      }
    }
  }

  @VisibleForTesting
  static RuleParamType guessType(Class<?> type) {
    return TYPE_FOR_CLASS.apply(type);
  }
}
