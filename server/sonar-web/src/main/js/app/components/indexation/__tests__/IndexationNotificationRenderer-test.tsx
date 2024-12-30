/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
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

import { byRole, byText } from '~sonar-aligned/helpers/testSelector';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { IndexationNotificationType } from '../../../../types/indexation';
import IndexationNotificationRenderer from '../IndexationNotificationRenderer';

describe('Indexation notification renderer', () => {
  const ui = {
    inProgressText: byText(/indexation.in_progress\s*indexation.features_partly_available/),
    completedText: byText('indexation.completed'),
    completedWithFailures: byText('indexation.completed_with_error'),
    docLink: byRole('link', { name: /indexation.features_partly_available.link/ }),
  };

  it('should display "In progress" status', () => {
    renderIndexationNotificationRenderer(IndexationNotificationType.InProgress);

    expect(ui.inProgressText.get()).toBeInTheDocument();
    expect(ui.docLink.get()).toBeInTheDocument();
  });

  it('should display "In progress with failures" status', () => {
    renderIndexationNotificationRenderer(IndexationNotificationType.InProgressWithFailure);

    expect(ui.inProgressText.get()).toBeInTheDocument();
    expect(ui.docLink.get()).toBeInTheDocument();
  });

  it('should display "Completed" status', () => {
    renderIndexationNotificationRenderer(IndexationNotificationType.Completed);

    expect(ui.completedText.get()).toBeInTheDocument();
  });

  it('should display "Completed with failures" status', () => {
    renderIndexationNotificationRenderer(IndexationNotificationType.CompletedWithFailure);

    expect(ui.completedWithFailures.get()).toBeInTheDocument();
  });
});

function renderIndexationNotificationRenderer(status: IndexationNotificationType) {
  renderComponent(
    <IndexationNotificationRenderer
      completedCount={23}
      onDismissBanner={() => undefined}
      shouldDisplaySurveyLink={false}
      total={42}
      type={status}
    />,
  );
}
