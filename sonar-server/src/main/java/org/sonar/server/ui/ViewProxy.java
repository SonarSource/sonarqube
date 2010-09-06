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
package org.sonar.server.ui;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.api.web.*;

public class ViewProxy<V extends View> implements Comparable<ViewProxy> {

  private V view;
  private String[] sections = {NavigationSection.HOME};
  private String[] userRoles = {};
  private String[] resourceScopes = {};
  private String[] resourceQualifiers = {};
  private String[] resourceLanguages = {};
  private String[] defaultForMetrics = {};
  private boolean isDefaultTab=false;
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
      if (defaultTabAnnotation==null || defaultTabAnnotation.metrics().length==0) {
        isDefaultTab = true;
        defaultForMetrics = new String[0];

      } else {
        isDefaultTab = false;
        defaultForMetrics = defaultTabAnnotation.metrics();
      }
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

  public boolean isWidget() {
    return isWidget;
  }

  public boolean isGwt() {
    return view instanceof GwtPage;
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