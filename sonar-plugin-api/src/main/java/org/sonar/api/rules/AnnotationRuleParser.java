/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.rules;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.PropertyType;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.api.utils.FieldUtils2;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * @since 2.3
 * @deprecated in 4.2. Replaced by {@link org.sonar.api.server.rule.RulesDefinitionAnnotationLoader}
 */
@Deprecated
@ServerSide
@ComputeEngineSide
public final class AnnotationRuleParser {

  private static final Logger LOG = Loggers.get(AnnotationRuleParser.class);

  public List<Rule> parse(String repositoryKey, Collection<Class> annotatedClasses) {
    List<Rule> rules = new ArrayList<>();
    for (Class annotatedClass : annotatedClasses) {
      rules.add(create(repositoryKey, annotatedClass));
    }
    return rules;
  }

  private static Rule create(String repositoryKey, Class annotatedClass) {
    org.sonar.check.Rule ruleAnnotation = AnnotationUtils.getAnnotation(annotatedClass, org.sonar.check.Rule.class);
    if (ruleAnnotation != null) {
      return toRule(repositoryKey, annotatedClass, ruleAnnotation);
    }
    LOG.warn("The class " + annotatedClass.getCanonicalName() + " should be annotated with " + Rule.class);
    return null;
  }

  private static Rule toRule(String repositoryKey, Class clazz, org.sonar.check.Rule ruleAnnotation) {
    String ruleKey = StringUtils.defaultIfEmpty(ruleAnnotation.key(), clazz.getCanonicalName());
    String ruleName = StringUtils.defaultIfEmpty(ruleAnnotation.name(), null);
    String description = StringUtils.defaultIfEmpty(ruleAnnotation.description(), null);
    Rule rule = Rule.create(repositoryKey, ruleKey, ruleName);
    rule.setDescription(description);
    rule.setSeverity(RulePriority.fromCheckPriority(ruleAnnotation.priority()));
    rule.setCardinality(ruleAnnotation.cardinality());
    rule.setStatus(ruleAnnotation.status());
    rule.setTags(ruleAnnotation.tags());

    List<Field> fields = FieldUtils2.getFields(clazz, true);
    for (Field field : fields) {
      addRuleProperty(rule, field);
    }
    return rule;
  }

  private static void addRuleProperty(Rule rule, Field field) {
    org.sonar.check.RuleProperty propertyAnnotation = field.getAnnotation(org.sonar.check.RuleProperty.class);
    if (propertyAnnotation != null) {
      String fieldKey = StringUtils.defaultIfEmpty(propertyAnnotation.key(), field.getName());
      RuleParam param = rule.createParameter(fieldKey);
      param.setDescription(propertyAnnotation.description());
      param.setDefaultValue(propertyAnnotation.defaultValue());
      if (!StringUtils.isBlank(propertyAnnotation.type())) {
        try {
          param.setType(PropertyType.valueOf(propertyAnnotation.type().trim()).name());
        } catch (IllegalArgumentException e) {
          throw new SonarException("Invalid property type [" + propertyAnnotation.type() + "]", e);
        }
      } else {
        param.setType(guessType(field.getType()).name());
      }
    }
  }

  private static final Function<Class<?>, PropertyType> TYPE_FOR_CLASS = Functions.forMap(
    ImmutableMap.<Class<?>, PropertyType>builder()
      .put(Integer.class, PropertyType.INTEGER)
      .put(int.class, PropertyType.INTEGER)
      .put(Float.class, PropertyType.FLOAT)
      .put(float.class, PropertyType.FLOAT)
      .put(Boolean.class, PropertyType.BOOLEAN)
      .put(boolean.class, PropertyType.BOOLEAN)
      .build(),
    PropertyType.STRING);

  static PropertyType guessType(Class<?> type) {
    return TYPE_FOR_CLASS.apply(type);
  }
}
