/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

import org.sonar.api.ServerExtension;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.action.Actions;
import org.sonar.api.issue.action.Function;
import org.sonar.api.issue.condition.HasIssuePropertyCondition;
import org.sonar.api.issue.condition.HasResolution;
import org.sonar.api.issue.condition.NotCondition;

public class ActionDefinition implements ServerExtension {

  public static final String FAKE_PROPERTY = "fake";

  private final Actions actions;

  public ActionDefinition(Actions actions) {
    this.actions = actions;
  }

  public void start() {
    actions.add("fake")
      .setConditions(
        new HasResolution(Issue.RESOLUTION_FIXED),
        new NotCondition(new HasIssuePropertyCondition(FAKE_PROPERTY))
      )
      .setFunctions(new Function() {
        @Override
        public void execute(Context context) {
          context.setAttribute(FAKE_PROPERTY, "fake action");
          context.addComment("New Comment from fake action");
        }
      });
  }
}
