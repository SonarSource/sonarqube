/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.core.issue;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.ActionPlanFinder;
import org.sonar.core.issue.db.ActionPlanDao;
import org.sonar.core.issue.db.ActionPlanDto;

import javax.annotation.Nullable;

import java.util.Collection;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 3.6
 */
public class DefaultActionPlanFinder implements ActionPlanFinder {

  private final ActionPlanDao actionPlanDao;

  public DefaultActionPlanFinder(ActionPlanDao actionPlanDao) {
    this.actionPlanDao = actionPlanDao;
  }

  public Collection<ActionPlan> findByKeys(Collection<String> keys) {
    Collection<ActionPlanDto> actionPlanDtos = actionPlanDao.findByKeys(keys);
    return newArrayList(Iterables.transform(actionPlanDtos, new Function<ActionPlanDto, ActionPlan>() {
      @Override
      public ActionPlan apply(@Nullable ActionPlanDto actionPlanDto) {
        return actionPlanDto.toActionPlan();
      }
    }));
  }
}
