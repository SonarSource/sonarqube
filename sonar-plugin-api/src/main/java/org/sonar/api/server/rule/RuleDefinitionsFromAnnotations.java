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
package org.sonar.api.server.rule;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.api.utils.FieldUtils2;
import org.sonar.check.Cardinality;

import javax.annotation.CheckForNull;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Read definitions of rules based on the annotations provided by sonar-check-api.
 * </p>
 * It is internally used by {@link RuleDefinitions} and can't be directly
 * used by plugins.
 *
 * @since 4.2
 */
class RuleDefinitionsFromAnnotations {

  private static final Logger LOG = LoggerFactory.getLogger(RuleDefinitionsFromAnnotations.class);

  void loadRules(RuleDefinitions.NewRepository repo, Class... annotatedClasses) {
    for (Class annotatedClass : annotatedClasses) {
      loadRule(repo, annotatedClass);
    }
  }

  @CheckForNull
  RuleDefinitions.NewRule loadRule(RuleDefinitions.NewRepository repo, Class clazz) {
    org.sonar.check.Rule ruleAnnotation = AnnotationUtils.getAnnotation(clazz, org.sonar.check.Rule.class);
    if (ruleAnnotation != null) {
      return loadRule(repo, clazz, ruleAnnotation);
    } else {
      LOG.warn("The class " + clazz.getCanonicalName() + " should be annotated with " + org.sonar.check.Rule.class);
      return null;
    }
  }

  private RuleDefinitions.NewRule loadRule(RuleDefinitions.NewRepository repo, Class clazz, org.sonar.check.Rule ruleAnnotation) {
    String ruleKey = StringUtils.defaultIfEmpty(ruleAnnotation.key(), clazz.getCanonicalName());
    String ruleName = StringUtils.defaultIfEmpty(ruleAnnotation.name(), null);
    String description = StringUtils.defaultIfEmpty(ruleAnnotation.description(), null);

    RuleDefinitions.NewRule rule = repo.createRule(ruleKey);
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

  private void loadParameters(RuleDefinitions.NewRule rule, Field field) {
    org.sonar.check.RuleProperty propertyAnnotation = field.getAnnotation(org.sonar.check.RuleProperty.class);
    if (propertyAnnotation != null) {
      String fieldKey = StringUtils.defaultIfEmpty(propertyAnnotation.key(), field.getName());
      RuleDefinitions.NewParam param = rule.createParam(fieldKey)
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

  private static final Function<Class<?>, RuleParamType> TYPE_FOR_CLASS = Functions.forMap(
    ImmutableMap.<Class<?>, RuleParamType>builder()
      .put(Integer.class, RuleParamType.INTEGER)
      .put(int.class, RuleParamType.INTEGER)
      .put(Float.class, RuleParamType.FLOAT)
      .put(float.class, RuleParamType.FLOAT)
      .put(Boolean.class, RuleParamType.BOOLEAN)
      .put(boolean.class, RuleParamType.BOOLEAN)
      .build(),
    RuleParamType.STRING);

  @VisibleForTesting
  static RuleParamType guessType(Class<?> type) {
    return TYPE_FOR_CLASS.apply(type);
  }
}
