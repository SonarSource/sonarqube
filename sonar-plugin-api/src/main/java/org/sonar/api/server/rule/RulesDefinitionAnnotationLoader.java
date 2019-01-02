/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  private static final Map<Class<?>, RuleParamType> TYPE_FOR_CLASS;

  static {
    Map<Class<?>, RuleParamType> map = new HashMap<>();
    map.put(Integer.class, RuleParamType.INTEGER);
    map.put(int.class, RuleParamType.INTEGER);
    map.put(Float.class, RuleParamType.FLOAT);
    map.put(float.class, RuleParamType.FLOAT);
    map.put(Boolean.class, RuleParamType.BOOLEAN);
    map.put(boolean.class, RuleParamType.BOOLEAN);
    TYPE_FOR_CLASS = Collections.unmodifiableMap(map);
  }

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

  static RuleParamType guessType(Class<?> type) {
    RuleParamType result = TYPE_FOR_CLASS.get(type);
    return result != null ? result : RuleParamType.STRING;
  }
}
