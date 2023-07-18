/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import { shallow } from 'enzyme';
import * as React from 'react';
import { mockCurrentUser } from '../../../../helpers/testMocks';
import { IndexationNotificationType } from '../../../../types/indexation';
import { IndexationNotification } from '../IndexationNotification';
import IndexationNotificationHelper from '../IndexationNotificationHelper';

beforeEach(() => jest.clearAllMocks());

jest.mock('../IndexationNotificationHelper');

describe('Completed banner', () => {
  it('should be displayed', () => {
    jest
      .mocked(IndexationNotificationHelper.shouldDisplayCompletedNotification)
      .mockReturnValueOnce(true);

    const wrapper = shallowRender();

    wrapper.setProps({
      indexationContext: {
        status: { hasFailures: false, isCompleted: true },
      },
    });

    expect(IndexationNotificationHelper.shouldDisplayCompletedNotification).toHaveBeenCalled();
    expect(wrapper.state().notificationType).toBe(IndexationNotificationType.Completed);
  });

  it('should be displayed at startup', () => {
    jest
      .mocked(IndexationNotificationHelper.shouldDisplayCompletedNotification)
      .mockReturnValueOnce(true);

    const wrapper = shallowRender({
      indexationContext: {
        status: { hasFailures: false, isCompleted: true },
      },
    });

    expect(IndexationNotificationHelper.shouldDisplayCompletedNotification).toHaveBeenCalled();
    expect(wrapper.state().notificationType).toBe(IndexationNotificationType.Completed);
  });

  it('should be hidden once completed without failure', () => {
    jest.useFakeTimers();

    jest
      .mocked(IndexationNotificationHelper.shouldDisplayCompletedNotification)
      .mockReturnValueOnce(true);

    const wrapper = shallowRender({
      indexationContext: {
        status: { hasFailures: false, isCompleted: true },
      },
    });

    expect(wrapper.state().notificationType).toBe(IndexationNotificationType.Completed);
    expect(IndexationNotificationHelper.markCompletedNotificationAsDisplayed).toHaveBeenCalled();

    jest.runAllTimers();

    expect(wrapper.state().notificationType).toBeUndefined();

    jest.useRealTimers();
  });
});

it('should display the completed-with-failure banner', () => {
  const wrapper = shallowRender({
    indexationContext: {
      status: { hasFailures: true, isCompleted: true },
    },
  });

  expect(wrapper.state().notificationType).toBe(IndexationNotificationType.CompletedWithFailure);
});

it('should display the progress banner', () => {
  const wrapper = shallowRender({
    indexationContext: {
      status: { completedCount: 23, hasFailures: false, isCompleted: false, total: 42 },
    },
  });

  expect(IndexationNotificationHelper.markCompletedNotificationAsToDisplay).toHaveBeenCalled();
  expect(wrapper.state().notificationType).toBe(IndexationNotificationType.InProgress);
});

it('should display the progress-with-failure banner', () => {
  const wrapper = shallowRender({
    indexationContext: {
      status: { completedCount: 23, hasFailures: true, isCompleted: false, total: 42 },
    },
  });

  expect(IndexationNotificationHelper.markCompletedNotificationAsToDisplay).toHaveBeenCalled();
  expect(wrapper.state().notificationType).toBe(IndexationNotificationType.InProgressWithFailure);
});

function shallowRender(props?: Partial<IndexationNotification['props']>) {
  return shallow<IndexationNotification>(
    <IndexationNotification
      currentUser={mockCurrentUser()}
      indexationContext={{
        status: { completedCount: 23, hasFailures: false, isCompleted: false, total: 42 },
      }}
      {...props}
    />
  );
}
