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
package org.sonar.api.checks.checkers;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.checks.profiles.Check;
import org.sonar.api.checks.profiles.CheckProfile;
import org.sonar.check.AnnotationIntrospector;
import org.sonar.check.CheckProperty;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @since 2.1 (experimental)
 * @deprecated since 2.3
 */
@Deprecated

public class AnnotationCheckerFactory<CHECKER> extends CheckerFactory<CHECKER> {

  private CheckProfile profile;
  private String repositoryKey;
  private Collection<Class<CHECKER>> checkerClasses;

  public AnnotationCheckerFactory(CheckProfile profile, String repositoryKey, Collection<Class<CHECKER>> checkerClasses) {
    this.profile = profile;
    this.repositoryKey = repositoryKey;
    this.checkerClasses = checkerClasses;
  }

  public Map<Check, CHECKER> create() {
    Map<String, Class<CHECKER>> classesByKey = getClassesByKey(checkerClasses);

    Map<Check, CHECKER> map = new IdentityHashMap<Check, CHECKER>();
    for (Check check : profile.getChecks(repositoryKey)) {
      Class<CHECKER> clazz = classesByKey.get(check.getTemplateKey());
      if (clazz != null) {
        CHECKER checker = instantiate(check, clazz);
        if (checker != null) {
          map.put(check, checker);
        }
      }
    }
    return map;
  }

  CHECKER instantiate(Check check, Class<CHECKER> clazz) {
    try {
      CHECKER checker = clazz.newInstance();
      configureFields(check, checker);
      return checker;

    } catch (UnvalidCheckerException e) {
      throw e;

    } catch (Exception e) {
      throw new UnvalidCheckerException("The checker " + clazz.getCanonicalName() + " can not be created", e);
    }
  }

  private void configureFields(Check check, CHECKER checker) throws IllegalAccessException {
    for (Map.Entry<String, String> entry : check.getProperties().entrySet()) {
      Field field = getField(checker, entry.getKey());
      if (field == null) {
        throw new UnvalidCheckerException("The field " + entry.getKey() + " does not exist or is not annotated with @CheckProperty");
      }
      if (StringUtils.isNotBlank(entry.getValue())) {
        configureField(checker, field, entry);
      }      
    }

  }

  private void configureField(Object checker, Field field, Map.Entry<String, String> parameter) throws IllegalAccessException {
    field.setAccessible(true);

    if (field.getType().equals(String.class)) {
      field.set(checker, parameter.getValue());

    } else if (field.getType().getSimpleName().equals("int")) {
      field.setInt(checker, Integer.parseInt(parameter.getValue()));

    } else if (field.getType().getSimpleName().equals("short")) {
      field.setShort(checker, Short.parseShort(parameter.getValue()));

    } else if (field.getType().getSimpleName().equals("long")) {
      field.setLong(checker, Long.parseLong(parameter.getValue()));

    } else if (field.getType().getSimpleName().equals("double")) {
      field.setDouble(checker, Double.parseDouble(parameter.getValue()));

    } else if (field.getType().getSimpleName().equals("boolean")) {
      field.setBoolean(checker, Boolean.parseBoolean(parameter.getValue()));

    } else if (field.getType().getSimpleName().equals("byte")) {
      field.setByte(checker, Byte.parseByte(parameter.getValue()));

    } else if (field.getType().equals(Integer.class)) {
      field.set(checker, new Integer(Integer.parseInt(parameter.getValue())));

    } else if (field.getType().equals(Long.class)) {
      field.set(checker, new Long(Long.parseLong(parameter.getValue())));

    } else if (field.getType().equals(Double.class)) {
      field.set(checker, new Double(Double.parseDouble(parameter.getValue())));

    } else if (field.getType().equals(Boolean.class)) {
      field.set(checker, Boolean.valueOf(Boolean.parseBoolean(parameter.getValue())));

    } else {
      throw new UnvalidCheckerException("The type of the field " + field + " is not supported: " + field.getType());
    }
  }

  private Field getField(Object checker, String key) {
    Field[] fields = checker.getClass().getDeclaredFields();
    for (Field field : fields) {
      CheckProperty annotation = field.getAnnotation(CheckProperty.class);
      if (annotation != null) {
        if (key.equals(field.getName()) || key.equals(annotation.key())) {
          return field;
        }
      }
    }
    return null;
  }

  private Map<String, Class<CHECKER>> getClassesByKey(Collection<Class<CHECKER>> checkerClasses) {
    Map<String, Class<CHECKER>> result = new HashMap<String, Class<CHECKER>>();
    for (Class<CHECKER> checkerClass : checkerClasses) {
      String key = AnnotationIntrospector.getCheckKey(checkerClass);
      if (key != null) {
        result.put(key, checkerClass);
      }
    }
    return result;
  }

}
