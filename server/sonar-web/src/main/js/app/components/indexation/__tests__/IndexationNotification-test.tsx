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
import { act } from '@testing-library/react';
import * as React from 'react';
import { byText } from '~sonar-aligned/helpers/testSelector';
import { mockCurrentUser, mockLoggedInUser } from '../../../../helpers/testMocks';
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

  it('should be hidden once completed without failure', () => {
    jest.useFakeTimers();

    jest
      .mocked(IndexationNotificationHelper.shouldDisplayCompletedNotification)
      .mockReturnValueOnce(true);

    renderIndexationNotification({
      indexationContext: {
        status: { hasFailures: false, isCompleted: true },
      },
    });

    expect(IndexationNotificationHelper.markCompletedNotificationAsDisplayed).toHaveBeenCalled();

    act(() => jest.runOnlyPendingTimers());

    expect(IndexationNotificationHelper.markCompletedNotificationAsDisplayed).toHaveBeenCalled();

    jest.useRealTimers();
  });

  it('should start progress > progress with failure > complete with failure', () => {
    const { rerender } = renderIndexationNotification({
      indexationContext: {
        status: { completedCount: 23, hasFailures: false, isCompleted: false, total: 42 },
      },
    });

    expect(byText('indexation.progression.23.42').get()).toBeInTheDocument();

    rerender(
      <IndexationNotification
        currentUser={mockLoggedInUser({ permissions: { global: [Permissions.Admin] } })}
        indexationContext={{
          status: { completedCount: 23, hasFailures: true, isCompleted: false, total: 42 },
        }}
      />,
    );

    expect(byText(/^indexation\.progression_with_error\.link/).get()).toBeInTheDocument();

    rerender(
      <IndexationNotification
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

    expect(byText('indexation.progression.23.42').get()).toBeInTheDocument();

    rerender(
      <IndexationNotification
        currentUser={mockLoggedInUser({ permissions: { global: [Permissions.Admin] } })}
        indexationContext={{
          status: { completedCount: 23, hasFailures: false, isCompleted: true, total: 42 },
        }}
      />,
    );
    expect(IndexationNotificationHelper.shouldDisplayCompletedNotification).toHaveBeenCalled();
  });

  it('should not see notification if not admin', () => {
    renderIndexationNotification({
      indexationContext: {
        status: { completedCount: 23, hasFailures: false, isCompleted: false, total: 42 },
      },
      currentUser: mockLoggedInUser(),
    });

    expect(byText('indexation.progression.23.42').query()).not.toBeInTheDocument();
  });
});

function renderIndexationNotification(props?: Partial<IndexationNotification['props']>) {
  return renderComponent(
    <IndexationNotification
      currentUser={mockLoggedInUser({ permissions: { global: [Permissions.Admin] } })}
      indexationContext={{
        status: { completedCount: 23, hasFailures: false, isCompleted: false, total: 42 },
      }}
      {...props}
    />,
  );
}
