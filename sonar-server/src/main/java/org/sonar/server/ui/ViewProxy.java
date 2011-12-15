/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.server.ui;

import com.google.common.collect.Maps;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.api.web.*;

import java.util.Collection;
import java.util.Map;

public class ViewProxy<V extends View> implements Comparable<ViewProxy> {

  private V view;
  private String[] sections = {NavigationSection.HOME};
  private String[] userRoles = {};
  private String[] resourceScopes = {};
  private String[] resourceQualifiers = {};
  private String[] resourceLanguages = {};
  private String[] defaultForMetrics = {};
  private String description = "";
  private Map<String, WidgetProperty> widgetPropertiesByKey = Maps.newHashMap();
  private String[] widgetCategories = {};
  private WidgetLayoutType widgetLayout = WidgetLayoutType.DEFAULT;
  private boolean isDefaultTab = false;
  private boolean isWidget = false;

  public ViewProxy(final V view) {
    this.view = view;

    UserRole userRoleAnnotation = AnnotationUtils.getClassAnnotation(view, UserRole.class);
    if (userRoleAnnotation != null) {
      userRoles = userRoleAnnotation.value();
    }

    NavigationSection sectionAnnotation = AnnotationUtils.getClassAnnotation(view, NavigationSection.class);
    if (sectionAnnotation != null) {
      sections = sectionAnnotation.value();
    }

    ResourceScope scopeAnnotation = AnnotationUtils.getClassAnnotation(view, ResourceScope.class);
    if (scopeAnnotation != null) {
      resourceScopes = scopeAnnotation.value();
    }

    ResourceQualifier qualifierAnnotation = AnnotationUtils.getClassAnnotation(view, ResourceQualifier.class);
    if (qualifierAnnotation != null) {
      resourceQualifiers = qualifierAnnotation.value();
    }

    ResourceLanguage languageAnnotation = AnnotationUtils.getClassAnnotation(view, ResourceLanguage.class);
    if (languageAnnotation != null) {
      resourceLanguages = languageAnnotation.value();
    }

    DefaultTab defaultTabAnnotation = AnnotationUtils.getClassAnnotation(view, DefaultTab.class);
    if (defaultTabAnnotation != null) {
      if (defaultTabAnnotation.metrics().length == 0) {
        isDefaultTab = true;
        defaultForMetrics = new String[0];

      } else {
        isDefaultTab = false;
        defaultForMetrics = defaultTabAnnotation.metrics();
      }
    }

    Description descriptionAnnotation = AnnotationUtils.getClassAnnotation(view, Description.class);
    if (descriptionAnnotation != null) {
      description = descriptionAnnotation.value();
    }

    WidgetProperties propAnnotation = AnnotationUtils.getClassAnnotation(view, WidgetProperties.class);
    if (propAnnotation != null) {
      for (WidgetProperty property : propAnnotation.value()) {
        widgetPropertiesByKey.put(property.key(), property);
      }
    }

    WidgetCategory categAnnotation = AnnotationUtils.getClassAnnotation(view, WidgetCategory.class);
    if (categAnnotation != null) {
      widgetCategories = categAnnotation.value();
    }

    WidgetLayout layoutAnnotation = AnnotationUtils.getClassAnnotation(view, WidgetLayout.class);
    if (layoutAnnotation != null) {
      widgetLayout = layoutAnnotation.value();
    }

    isWidget = (view instanceof Widget);
  }

  public V getTarget() {
    return view;
  }

  public String getId() {
    return view.getId();
  }

  public String getTitle() {
    return view.getTitle();
  }

  public String getDescription() {
    return description;
  }

  public Collection<WidgetProperty> getWidgetProperties() {
    return widgetPropertiesByKey.values();
  }

  public WidgetProperty getWidgetProperty(String propertyKey) {
    return widgetPropertiesByKey.get(propertyKey);
  }

  public String[] getWidgetCategories() {
    return widgetCategories;
  }

  public String[] getSections() {
    return sections;
  }

  public String[] getUserRoles() {
    return userRoles;
  }

  public String[] getResourceScopes() {
    return resourceScopes;
  }

  public String[] getResourceQualifiers() {
    return resourceQualifiers;
  }

  public String[] getResourceLanguages() {
    return resourceLanguages;
  }

  public boolean isDefaultTab() {
    return isDefaultTab;
  }

  public String[] getDefaultTabForMetrics() {
    return defaultForMetrics;
  }

  public boolean supportsMetric(String metricKey) {
    return ArrayUtils.contains(defaultForMetrics, metricKey);
  }

  public boolean isWidget() {
    return isWidget;
  }

  public boolean isGwt() {
    return view instanceof GwtPage;
  }

  public WidgetLayoutType getWidgetLayout() {
    return widgetLayout;
  }

  public boolean isEditable() {
    return !widgetPropertiesByKey.isEmpty();
  }

  public boolean hasRequiredProperties() {
    boolean requires = false;
    for (WidgetProperty property : getWidgetProperties()) {
      if (!property.optional() && StringUtils.isEmpty(property.defaultValue())) {
        requires = true;
      }
    }
    return requires;
  }

  @Override
  public int hashCode() {
    return getId().hashCode();
  }


  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (obj.getClass() != getClass()) {
      return false;
    }
    ViewProxy rhs = (ViewProxy) obj;
    return new EqualsBuilder()
      .append(getId(), rhs.getId())
      .isEquals();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("id", view.getId())
      .append("sections", sections)
      .append("userRoles", userRoles)
      .append("scopes", resourceScopes)
      .append("qualifiers", resourceQualifiers)
      .append("languages", resourceLanguages)
      .append("metrics", defaultForMetrics)
      .toString();

  }

  public int compareTo(ViewProxy other) {
    return new CompareToBuilder()
      .append(getTitle(), other.getTitle())
      .append(getId(), other.getId())
      .toComparison();

  }
}