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
package org.sonar.api.batch.rule;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.api.utils.FieldUtils2;
import org.sonar.api.utils.SonarException;
import org.sonar.check.RuleProperty;

/**
 * Instantiates checks (objects that provide implementation of coding
 * rules) that use sonar-check-api annotations. Checks are selected and configured
 * from the Quality profiles enabled on the current module.
 * <br>
 * Example of check class:
 * <pre>
 *   {@literal @}org.sonar.check.Rule(key = "S001")
 *   public class CheckS001 {
 *     {@literal @}org.sonar.check.RuleProperty
 *     private String pattern;
 *
 *     public String getPattern() {
 *       return pattern;
 *     }
 * }
 * </pre>
 * How to use:
 * <pre>
 *   public class MyRuleEngine extends BatchExtension {
 *     private final CheckFactory checkFactory;
 *
 *     public MyRuleEngine(CheckFactory checkFactory) {
 *       this.checkFactory = checkFactory;
 *     }
 *
 *     public void execute() {
 *       Checks checks = checkFactory.create("my-rule-repository");
 *       checks.addAnnotatedChecks(CheckS001.class);
 *       // checks.all() contains an instance of CheckS001
 *       // with field "pattern" set to the value specified in
 *       // the Quality profile
 *
 *       // Checks are used to detect issues on source code
 *
 *       // checks.ruleKey(obj) can be used to create the related issues
 *     }
 *   }
 * </pre>
 * <br>
 * It replaces org.sonar.api.checks.AnnotationCheckFactory
 *
 * @since 4.2
 */
public class Checks<C> {
  private final ActiveRules activeRules;
  private final String repository;
  private final Map<RuleKey, C> checkByRule = new HashMap<>();
  private final Map<C, RuleKey> ruleByCheck = new IdentityHashMap<>();

  Checks(ActiveRules activeRules, String repository) {
    this.activeRules = activeRules;
    this.repository = repository;
  }

  @CheckForNull
  public C of(RuleKey ruleKey) {
    return checkByRule.get(ruleKey);
  }

  public Collection<C> all() {
    return checkByRule.values();
  }

  @CheckForNull
  public RuleKey ruleKey(C check) {
    return ruleByCheck.get(check);
  }

  private void add(RuleKey ruleKey, C obj) {
    checkByRule.put(ruleKey, obj);
    ruleByCheck.put(obj, ruleKey);
  }

  public Checks<C> addAnnotatedChecks(Object... checkClassesOrObjects) {
    return addAnnotatedChecks((Iterable) Arrays.asList(checkClassesOrObjects));
  }

  /**
   * @deprecated since 5.2 use {@link #addAnnotatedChecks(Iterable)}
   */
  @Deprecated
  public Checks<C> addAnnotatedChecks(Collection checkClassesOrObjects) {
    return addAnnotatedChecks((Iterable) checkClassesOrObjects);
  }

  public Checks<C> addAnnotatedChecks(Iterable checkClassesOrObjects) {
    Map<String, Object> checksByEngineKey = new HashMap<>();
    for (Object checkClassesOrObject : checkClassesOrObjects) {
      String engineKey = annotatedEngineKey(checkClassesOrObject);
      if (engineKey != null) {
        checksByEngineKey.put(engineKey, checkClassesOrObject);
      }
    }

    for (ActiveRule activeRule : activeRules.findByRepository(repository)) {
      String engineKey = StringUtils.defaultIfBlank(activeRule.templateRuleKey(), activeRule.ruleKey().rule());
      Object checkClassesOrObject = checksByEngineKey.get(engineKey);
      if (checkClassesOrObject != null) {
        Object obj = instantiate(activeRule, checkClassesOrObject);
        add(activeRule.ruleKey(), (C) obj);
      }
    }
    return this;
  }

  private static String annotatedEngineKey(Object annotatedClassOrObject) {
    String key = null;
    org.sonar.check.Rule ruleAnnotation = AnnotationUtils.getAnnotation(annotatedClassOrObject, org.sonar.check.Rule.class);
    if (ruleAnnotation != null) {
      key = ruleAnnotation.key();
    }
    Class clazz = annotatedClassOrObject.getClass();
    if (annotatedClassOrObject instanceof Class) {
      clazz = (Class) annotatedClassOrObject;
    }
    return StringUtils.defaultIfEmpty(key, clazz.getCanonicalName());
  }

  private static Object instantiate(ActiveRule activeRule, Object checkClassOrInstance) {
    try {
      Object check = checkClassOrInstance;
      if (check instanceof Class) {
        check = ((Class) checkClassOrInstance).newInstance();
      }
      configureFields(activeRule, check);
      return check;
    } catch (InstantiationException | IllegalAccessException e) {
      throw failToInstantiateCheck(activeRule, checkClassOrInstance, e);
    }
  }

  private static RuntimeException failToInstantiateCheck(ActiveRule activeRule, Object checkClassOrInstance, Exception e) {
    throw new IllegalStateException(String.format("Fail to instantiate class %s for rule %s", checkClassOrInstance, activeRule.ruleKey()), e);
  }

  private static void configureFields(ActiveRule activeRule, Object check) {
    for (Map.Entry<String, String> param : activeRule.params().entrySet()) {
      Field field = getField(check, param.getKey());
      if (field == null) {
        throw new IllegalStateException(
          String.format("The field '%s' does not exist or is not annotated with @RuleProperty in the class %s", param.getKey(), check.getClass().getName()));
      }
      if (StringUtils.isNotBlank(param.getValue())) {
        configureField(check, field, param.getValue());
      }
    }
  }

  @CheckForNull
  private static Field getField(Object check, String key) {
    List<Field> fields = FieldUtils2.getFields(check.getClass(), true);
    for (Field field : fields) {
      RuleProperty propertyAnnotation = field.getAnnotation(RuleProperty.class);
      if (propertyAnnotation != null && (StringUtils.equals(key, field.getName()) || StringUtils.equals(key, propertyAnnotation.key()))) {
        return field;
      }
    }
    return null;
  }

  private static void configureField(Object check, Field field, String value) {
    try {
      field.setAccessible(true);

      if (field.getType().equals(String.class)) {
        field.set(check, value);

      } else if (int.class == field.getType()) {
        field.setInt(check, Integer.parseInt(value));

      } else if (short.class == field.getType()) {
        field.setShort(check, Short.parseShort(value));

      } else if (long.class == field.getType()) {
        field.setLong(check, Long.parseLong(value));

      } else if (double.class == field.getType()) {
        field.setDouble(check, Double.parseDouble(value));

      } else if (boolean.class == field.getType()) {
        field.setBoolean(check, Boolean.parseBoolean(value));

      } else if (byte.class == field.getType()) {
        field.setByte(check, Byte.parseByte(value));

      } else if (Integer.class == field.getType()) {
        field.set(check, Integer.parseInt(value));

      } else if (Long.class == field.getType()) {
        field.set(check, Long.parseLong(value));

      } else if (Double.class == field.getType()) {
        field.set(check, Double.parseDouble(value));

      } else if (Boolean.class == field.getType()) {
        field.set(check, Boolean.parseBoolean(value));

      } else {
        throw new SonarException("The type of the field " + field + " is not supported: " + field.getType());
      }
    } catch (IllegalAccessException e) {
      throw new SonarException("Can not set the value of the field " + field + " in the class: " + check.getClass().getName(), e);
    }
  }
}
