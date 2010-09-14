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
package org.sonar.api.checks.templates;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.check.AnnotationIntrospector;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @since 2.1 (experimental)
 * @deprecated since 2.3
 */
@Deprecated
public class AnnotationCheckTemplateFactory {

  private static final Logger LOG = LoggerFactory.getLogger(AnnotationCheckTemplateFactory.class);

  private Collection<Class> annotatedClasses;

  public AnnotationCheckTemplateFactory(Collection<Class> annotatedClasses) {
    this.annotatedClasses = annotatedClasses;
  }

  public List<CheckTemplate> create() {
    List<CheckTemplate> templates = new ArrayList<CheckTemplate>();
    for (Class annotatedClass : annotatedClasses) {
      BundleCheckTemplate template = create(annotatedClass);
      if (template != null) {
        templates.add(template);
      }
    }
    return templates;
  }


  protected BundleCheckTemplate create(Class annotatedClass) {
    org.sonar.check.Check checkAnnotation = AnnotationIntrospector.getCheckAnnotation(annotatedClass);
    if (checkAnnotation == null) {
      LOG.warn("The class " + annotatedClass.getCanonicalName() + " is not a check template. It should be annotated with " + CheckTemplate.class);
      return null;
    }

    BundleCheckTemplate check = toTemplate(annotatedClass, checkAnnotation);
    Field[] fields = annotatedClass.getDeclaredFields();
    if (fields != null) {
      for (Field field : fields) {
        BundleCheckTemplateProperty property = toProperty(check, field);
        if (property != null) {
          check.addProperty(property);
        }
      }
    }
    return check;
  }

  private static BundleCheckTemplate toTemplate(Class annotatedClass, org.sonar.check.Check checkAnnotation) {
    String key = AnnotationIntrospector.getCheckKey(annotatedClass);
    String bundle = getBundleBaseName(checkAnnotation, annotatedClass);

    BundleCheckTemplate check = new BundleCheckTemplate(key, bundle);
    check.setDefaultDescription(checkAnnotation.description());
    check.setDefaultTitle(checkAnnotation.title());
    check.setIsoCategory(checkAnnotation.isoCategory());
    check.setPriority(checkAnnotation.priority());

    return check;
  }

  private static String getBundleBaseName(org.sonar.check.Check checkAnnotation, Class annotatedClass) {
    String bundle = checkAnnotation.bundle();
    if (StringUtils.isBlank(bundle)) {
      bundle = annotatedClass.getCanonicalName();
    }
    return bundle;
  }

  private static BundleCheckTemplateProperty toProperty(BundleCheckTemplate check, Field field) {
    org.sonar.check.CheckProperty propertyAnnotation = field.getAnnotation(org.sonar.check.CheckProperty.class);
    if (propertyAnnotation != null) {
      String fieldKey = propertyAnnotation.key();
      if (fieldKey==null || "".equals(fieldKey)) {
        fieldKey = field.getName();
      }
      BundleCheckTemplateProperty property = new BundleCheckTemplateProperty(check, fieldKey);
      property.setDefaultTitle(propertyAnnotation.title());
      property.setDefaultDescription(propertyAnnotation.description());
      return property;
    }
    return null;
  }
}
