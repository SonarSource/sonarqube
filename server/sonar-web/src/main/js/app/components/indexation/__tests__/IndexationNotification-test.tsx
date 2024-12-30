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
import { mockAppState, mockCurrentUser, mockLoggedInUser } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { Permissions } from '../../../../types/permissions';
import { IndexationNotification } from '../IndexationNotification';
import IndexationNotificationHelper from '../IndexationNotificationHelper';

beforeEach(() => jest.clearAllMocks());

jest.mock('../IndexationNotificationHelper');

describe('Completed banner', () => {
  it('should be displayed and call helper when updated', () => {
    jest
      .mocked(IndexationNotificationHelper.shouldDisplayCompletedNotification)
      .mockReturnValueOnce(true);

    const { rerender } = renderIndexationNotification();

    rerender(
      <IndexationNotification
        appState={mockAppState()}
        currentUser={mockCurrentUser()}
        indexationContext={{
          status: { completedCount: 23, hasFailures: false, isCompleted: true, total: 42 },
        }}
      />,
    );

    expect(IndexationNotificationHelper.shouldDisplayCompletedNotification).toHaveBeenCalled();
  });

  it('should be displayed at startup', () => {
    jest
      .mocked(IndexationNotificationHelper.shouldDisplayCompletedNotification)
      .mockReturnValueOnce(true);

    renderIndexationNotification({
      indexationContext: {
        status: { hasFailures: false, isCompleted: true },
      },
    });

    expect(IndexationNotificationHelper.shouldDisplayCompletedNotification).toHaveBeenCalled();
  });

  it('should start progress > progress with failure > complete with failure', () => {
    const { rerender } = renderIndexationNotification({
      indexationContext: {
        status: { completedCount: 23, hasFailures: false, isCompleted: false, total: 42 },
      },
    });

    expect(byText('indexation.progression2342').get()).toBeInTheDocument();

    rerender(
      <IndexationNotification
        appState={mockAppState()}
        currentUser={mockLoggedInUser({ permissions: { global: [Permissions.Admin] } })}
        indexationContext={{
          status: { completedCount: 23, hasFailures: true, isCompleted: false, total: 42 },
        }}
      />,
    );

    expect(byText(/^indexation\.progression_with_error\.link/).get()).toBeInTheDocument();

    rerender(
      <IndexationNotification
        appState={mockAppState()}
        currentUser={mockLoggedInUser({ permissions: { global: [Permissions.Admin] } })}
        indexationContext={{
          status: { completedCount: 23, hasFailures: true, isCompleted: true, total: 42 },
        }}
      />,
    );
    expect(byText('indexation.completed_with_error').get()).toBeInTheDocument();
  });

  it('should start progress > success > disappear', () => {
    const { rerender } = renderIndexationNotification({
      indexationContext: {
        status: { completedCount: 23, hasFailures: false, isCompleted: false, total: 42 },
      },
    });

    expect(byText('indexation.progression2342').get()).toBeInTheDocument();

    rerender(
      <IndexationNotification
        appState={mockAppState()}
        currentUser={mockLoggedInUser({ permissions: { global: [Permissions.Admin] } })}
        indexationContext={{
          status: { completedCount: 23, hasFailures: false, isCompleted: true, total: 42 },
        }}
      />,
    );
    expect(IndexationNotificationHelper.shouldDisplayCompletedNotification).toHaveBeenCalled();
  });

  it('should show survey link when indexation follows an upgrade', () => {
    jest
      .mocked(IndexationNotificationHelper.shouldDisplayCompletedNotification)
      .mockReturnValueOnce(true);
    jest
      .mocked(IndexationNotificationHelper.getLastIndexationSQSVersion)
      .mockReturnValueOnce('11.0');

    const { rerender } = renderIndexationNotification({
      appState: mockAppState({ version: '12.0' }),
      indexationContext: {
        status: { completedCount: 42, hasFailures: false, isCompleted: true, total: 42 },
      },
    });

    expect(byText('indexation.upgrade_survey_link').get()).toBeInTheDocument();

    rerender(
      <IndexationNotification
        appState={mockAppState({ version: '12.0' })}
        currentUser={mockLoggedInUser({ permissions: { global: [Permissions.Admin] } })}
        indexationContext={{
          status: { completedCount: 23, hasFailures: true, isCompleted: true, total: 42 },
        }}
      />,
    );

    expect(byText('indexation.upgrade_survey_link').get()).toBeInTheDocument();
  });

  it('should not show survey link when indexation does not follow an upgrade', () => {
    jest
      .mocked(IndexationNotificationHelper.shouldDisplayCompletedNotification)
      .mockReturnValueOnce(true);
    jest
      .mocked(IndexationNotificationHelper.getLastIndexationSQSVersion)
      .mockReturnValueOnce('12.0');

    const { rerender } = renderIndexationNotification({
      appState: mockAppState({ version: '12.0' }),
      indexationContext: {
        status: { completedCount: 42, hasFailures: false, isCompleted: true, total: 42 },
      },
    });

    expect(byRole('indexation.upgrade_survey_link').query()).not.toBeInTheDocument();

    rerender(
      <IndexationNotification
        appState={mockAppState({ version: '12.0' })}
        currentUser={mockLoggedInUser({ permissions: { global: [Permissions.Admin] } })}
        indexationContext={{
          status: { completedCount: 23, hasFailures: true, isCompleted: true, total: 42 },
        }}
      />,
    );

    expect(byRole('indexation.upgrade_survey_link').query()).not.toBeInTheDocument();
  });

  it('should not see notification if not admin', () => {
    renderIndexationNotification({
      indexationContext: {
        status: { completedCount: 23, hasFailures: false, isCompleted: false, total: 42 },
      },
      currentUser: mockLoggedInUser(),
    });

    expect(byText('indexation.progression2342').query()).not.toBeInTheDocument();
  });
});

function renderIndexationNotification(props?: Partial<IndexationNotification['props']>) {
  return renderComponent(
    <IndexationNotification
      appState={mockAppState()}
      currentUser={mockLoggedInUser({ permissions: { global: [Permissions.Admin] } })}
      indexationContext={{
        status: { completedCount: 23, hasFailures: false, isCompleted: false, total: 42 },
      }}
      {...props}
    />,
  );
}
