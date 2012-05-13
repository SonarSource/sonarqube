/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.api.reviews;

import com.google.common.annotations.Beta;
import com.google.common.collect.Maps;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Map;

/**
 * @since 3.1
 */
@Beta
public final class ReviewContext {

  private static final String PROJECT_KEY = "project";
  private static final String REVIEW_KEY = "review";
  private static final String USER_KEY = "user";
  private static final String PARAMS_KEY = "params";

  private Map<String, String> projectProps = Maps.newHashMap();
  private Map<String, String> reviewProps = Maps.newHashMap();
  private Map<String, String> userProps = Maps.newHashMap();
  private Map<String, String> paramsProps = Maps.newHashMap();

  private ReviewContext() {
  }

  public static ReviewContext createFromMap(Map<String, Map<String, String>> propertiesMap) {
    ReviewContext context = new ReviewContext();
    if (propertiesMap.get(PROJECT_KEY) != null) {
      context.projectProps = propertiesMap.get(PROJECT_KEY);
    }
    if (propertiesMap.get(REVIEW_KEY) != null) {
      context.reviewProps = propertiesMap.get(REVIEW_KEY);
    }
    if (propertiesMap.get(USER_KEY) != null) {
      context.userProps = propertiesMap.get(USER_KEY);
    }
    if (propertiesMap.get(PARAMS_KEY) != null) {
      context.paramsProps = propertiesMap.get(PARAMS_KEY);
    }
    return context;
  }

  public String getProjectProperty(String propertyKey) {
    return projectProps.get(propertyKey);
  }

  public String getReviewProperty(String propertyKey) {
    return reviewProps.get(propertyKey);
  }

  public String getUserProperty(String propertyKey) {
    return userProps.get(propertyKey);
  }

  public String getParamValue(String paramKey) {
    return paramsProps.get(paramKey);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

}
