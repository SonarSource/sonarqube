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
package org.sonar.server.reviews;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.sonar.api.reviews.ReviewAction;
import org.sonar.api.utils.SonarException;

import java.util.Collection;
import java.util.Map;

/**
 * @since 3.1
 */
public class ReviewActionsManager {

  private Map<String, ReviewAction> idToAction = Maps.newHashMap();
  private Map<String, Collection<ReviewAction>> interfaceToAction = Maps.newHashMap();

  public ReviewActionsManager(ReviewAction[] reviewActions) {
    for (ReviewAction reviewAction : reviewActions) {
      idToAction.put(reviewAction.getId(), reviewAction);
    }
  }

  public ReviewActionsManager() {
    this(new ReviewAction[0]);
  }

  public ReviewAction getAction(String actionId) {
    return idToAction.get(actionId);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public Collection<ReviewAction> getActions(String interfaceName) {
    Collection<ReviewAction> result = interfaceToAction.get(interfaceName);
    if (result == null) {
      result = Lists.newArrayList();
      interfaceToAction.put(interfaceName, result);
      try {
        Class interfaceClass = Class.forName(interfaceName);
        for (ReviewAction reviewAction : idToAction.values()) {
          if (interfaceClass.isAssignableFrom(reviewAction.getClass())) {
            result.add(reviewAction);
          }
        }
      } catch (ClassNotFoundException e) {
        throw new SonarException("The following interface for review actions does not exist: " + interfaceName);
      }

    }
    return result;
  }

}
