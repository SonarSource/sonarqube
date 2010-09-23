/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.api.rules;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.check.Check;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

/**
 * @since 2.3
 */
public final class AnnotationRuleRepository extends RuleRepository {

  private static final Logger LOG = LoggerFactory.getLogger(AnnotationRuleRepository.class);

  private Collection<Class> annotatedClasses;

  /**
   * Use the factory method()
   */
  private AnnotationRuleRepository(String key, String language, Collection<Class> annotatedClasses) {
    super(key, language);
    this.annotatedClasses = annotatedClasses;
  }

  public static AnnotationRuleRepository create(String key, String language, Collection<Class> annotatedClasses) {
    return new AnnotationRuleRepository(key, language, annotatedClasses);
  }

  @Override
  public List<Rule> createRules() {
    List<Rule> rules = Lists.newArrayList();
    for (Class annotatedClass : annotatedClasses) {
      rules.add(create(annotatedClass));
    }
    return rules;
  }

  private Rule create(Class annotatedClass) {
    org.sonar.check.Rule ruleAnnotation = AnnotationUtils.getClassAnnotation(annotatedClass, org.sonar.check.Rule.class);
    if (ruleAnnotation != null) {
      return toRule(annotatedClass, ruleAnnotation);
    }
    Check checkAnnotation = AnnotationUtils.getClassAnnotation(annotatedClass, Check.class);
    if (checkAnnotation != null) {
      return toRule(annotatedClass, checkAnnotation);
    }
    LOG.warn("The class " + annotatedClass.getCanonicalName() + " is not a check template. It should be annotated with " + Rule.class);
    return null;
  }

  private Rule toRule(Class clazz, org.sonar.check.Rule ruleAnnotation) {
    String key = StringUtils.defaultIfEmpty(ruleAnnotation.key(), clazz.getCanonicalName());
    Rule rule = Rule.create(getKey(), key, ruleAnnotation.name());
    rule.setDescription(ruleAnnotation.description());
    rule.setRulesCategory(RulesCategory.fromIsoCategory(ruleAnnotation.isoCategory()));
    rule.setPriority(RulePriority.fromCheckPriority(ruleAnnotation.priority()));

    Field[] fields = clazz.getDeclaredFields();
    if (fields != null) {
      for (Field field : fields) {
        addRuleProperty(rule, field);
      }
    }

    return rule;
  }

  private Rule toRule(Class clazz, Check checkAnnotation) {
    String key = StringUtils.defaultIfEmpty(checkAnnotation.key(), clazz.getCanonicalName());
    Rule rule = Rule.create(getKey(), key, checkAnnotation.title());
    rule.setDescription(checkAnnotation.description());
    rule.setRulesCategory(RulesCategory.fromIsoCategory(checkAnnotation.isoCategory()));
    rule.setPriority(RulePriority.fromCheckPriority(checkAnnotation.priority()));

    Field[] fields = clazz.getDeclaredFields();
    if (fields != null) {
      for (Field field : fields) {
        addCheckProperty(rule, field);
      }
    }
    return rule;
  }

  private void addRuleProperty(Rule rule, Field field) {
    org.sonar.check.RuleProperty propertyAnnotation = field.getAnnotation(org.sonar.check.RuleProperty.class);
    if (propertyAnnotation != null) {
      String fieldKey = StringUtils.defaultIfEmpty(propertyAnnotation.key(), field.getName());
      RuleParam param = rule.createParameter(fieldKey);
      param.setDescription(propertyAnnotation.description());
      param.setDefaultValue(propertyAnnotation.defaultValue());
    }
  }

  private void addCheckProperty(Rule rule, Field field) {
    org.sonar.check.CheckProperty propertyAnnotation = field.getAnnotation(org.sonar.check.CheckProperty.class);
    if (propertyAnnotation != null) {
      String fieldKey = StringUtils.defaultIfEmpty(propertyAnnotation.key(), field.getName());
      RuleParam param = rule.createParameter(fieldKey);
      param.setDescription(propertyAnnotation.description());
    }
  }
}
