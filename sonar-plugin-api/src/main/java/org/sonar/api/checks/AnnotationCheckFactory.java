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
package org.sonar.api.checks;

import com.google.common.collect.Maps;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.rule.Checks;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.api.utils.FieldUtils2;
import org.sonar.api.utils.SonarException;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;

/**
 * @since 2.3
 * @deprecated since 4.2 use {@link Checks}
 */
@Deprecated
public final class AnnotationCheckFactory extends CheckFactory {

  private static final String CAN_NOT_INSTANTIATE_THE_CHECK_RELATED_TO_THE_RULE = "Can not instantiate the check related to the rule ";
  private Map<String, Object> checksByKey = Maps.newHashMap();

  private AnnotationCheckFactory(RulesProfile profile, String repositoryKey, Collection checks) {
    super(profile, repositoryKey);
    groupByKey(checks);
  }

  public static AnnotationCheckFactory create(RulesProfile profile, String repositoryKey, Collection checkClasses) {
    AnnotationCheckFactory factory = new AnnotationCheckFactory(profile, repositoryKey, checkClasses);
    factory.init();
    return factory;
  }

  private void groupByKey(Collection checks) {
    for (Object check : checks) {
      String key = getRuleKey(check);
      if (key != null) {
        checksByKey.put(key, check);
      }
    }
  }

  @Override
  public Object createCheck(ActiveRule activeRule) {
    Object object = checksByKey.get(activeRule.getConfigKey());
    if (object != null) {
      return instantiate(activeRule, object);
    }
    return null;
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
      throw new SonarException(CAN_NOT_INSTANTIATE_THE_CHECK_RELATED_TO_THE_RULE + activeRule, e);

    }
  }

  private static void configureFields(ActiveRule activeRule, Object check) {
    for (ActiveRuleParam param : activeRule.getActiveRuleParams()) {
      Field field = getField(check, param.getKey());
      if (field == null) {
        throw new SonarException("The field " + param.getKey() + " does not exist or is not annotated with @RuleProperty in the class " + check.getClass().getName());
      }
      if (StringUtils.isNotBlank(param.getValue())) {
        configureField(check, field, param.getValue());
      }
    }

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

  private static String getRuleKey(Object annotatedClassOrObject) {
    String key = null;
    Rule ruleAnnotation = AnnotationUtils.getAnnotation(annotatedClassOrObject, Rule.class);
    if (ruleAnnotation != null) {
      key = ruleAnnotation.key();
    }
    Class clazz = annotatedClassOrObject.getClass();
    if (annotatedClassOrObject instanceof Class) {
      clazz = (Class) annotatedClassOrObject;
    }
    return StringUtils.defaultIfEmpty(key, clazz.getCanonicalName());
  }
}
