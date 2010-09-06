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
package org.sonar.api.test;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.sonar.api.resources.Resource;

public class IsResource extends BaseMatcher<Resource> {

  private String scope;
  private String qualifier;
  private String key;

  public IsResource(String scope, String qualifier) {
    this.scope = scope;
    this.qualifier = qualifier;
  }

  public IsResource(String scope, String qualifier, String key) {
    this(scope, qualifier);
    this.key = key;
  }

  public boolean matches(Object o) {
    Resource r = (Resource) o;
    boolean keyMatch = (key != null) ? StringUtils.equals(r.getKey(), key) : true;
    return ObjectUtils.equals(r.getScope(), scope) && ObjectUtils.equals(r.getQualifier(), qualifier) && keyMatch;
  }

  public void describeTo(Description description) {

  }
}