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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.check.AnnotationIntrospector;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @since 2.3
 */
public final class RuleAnnotationUtils {

  private static final Logger LOG = LoggerFactory.getLogger(RuleAnnotationUtils.class);

  private RuleAnnotationUtils() {
    // only static methods
  }

  public static List<Rule> readAnnotatedClasses(Collection<Class> annotatedClasses) {
    List<Rule> rules = new ArrayList<Rule>();
    if (annotatedClasses != null) {
      for (Class annotatedClass : annotatedClasses) {
        Rule rule = readAnnotatedClass(annotatedClass);
        if (rule != null) {
          rules.add(rule);
        }
      }
    }
    return rules;
  }

  public static Rule readAnnotatedClass(Class annotatedClass) {
    org.sonar.check.Check checkAnnotation = AnnotationIntrospector.getCheckAnnotation(annotatedClass);
    if (checkAnnotation == null) {
      LOG.warn("The class " + annotatedClass.getCanonicalName() + " is not a rule. It should be annotated with " + org.sonar.check.Check.class);
      return null;
    }

    Rule rule = toRule(annotatedClass, checkAnnotation);
    Field[] fields = annotatedClass.getDeclaredFields();
    if (fields != null) {
      for (Field field : fields) {
        createParam(rule, field);
      }
    }
    return rule;
  }

  private static Rule toRule(Class annotatedClass, org.sonar.check.Check annotation) {
    String key = AnnotationIntrospector.getCheckKey(annotatedClass);

    Rule rule = Rule.create();
    rule.setKey(key);
    rule.setName(annotation.title());
    rule.setDescription(annotation.description());
    rule.setRulesCategory(new RulesCategory(annotation.isoCategory().name()));
    rule.setPriority(RulePriority.fromCheckPriority(annotation.priority()));
    return rule;
  }

  private static void createParam(Rule rule, Field field) {
    org.sonar.check.CheckProperty propertyAnnotation = field.getAnnotation(org.sonar.check.CheckProperty.class);
    if (propertyAnnotation != null) {
      String fieldKey = propertyAnnotation.key();
      if (fieldKey==null || "".equals(fieldKey)) {
        fieldKey = field.getName();
      }
      RuleParam param = rule.createParameter(fieldKey);
      param.setDescription(propertyAnnotation.description());
    }
  }
}
