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
import { setImmediate } from 'timers';
import { getIndexationStatus } from '../../../../api/ce';
import { get, remove, save } from '../../../../helpers/storage';
import { IndexationStatus } from '../../../../types/indexation';
import IndexationNotificationHelper from '../IndexationNotificationHelper';

beforeEach(() => {
  jest.clearAllMocks();
  jest.useFakeTimers();
});

afterEach(() => {
  jest.runOnlyPendingTimers();
  jest.useRealTimers();
});

jest.mock('../../../../api/ce', () => ({
  getIndexationStatus: jest.fn().mockResolvedValue({}),
}));

jest.mock('../../../../helpers/storage', () => ({
  get: jest.fn(),
  remove: jest.fn(),
  save: jest.fn(),
}));

it('should properly start & stop polling for indexation status', async () => {
  const onNewStatus = jest.fn();

  const newStatus: IndexationStatus = {
    completedCount: 23,
    hasFailures: false,
    isCompleted: false,
    total: 42,
  };

  jest.mocked(getIndexationStatus).mockResolvedValueOnce(newStatus);

  IndexationNotificationHelper.startPolling(onNewStatus);
  expect(getIndexationStatus).toHaveBeenCalled();

  await new Promise(setImmediate);
  expect(onNewStatus).toHaveBeenCalledWith(newStatus);

  jest.runOnlyPendingTimers();
  expect(getIndexationStatus).toHaveBeenCalledTimes(2);

  jest.mocked(getIndexationStatus).mockClear();

  IndexationNotificationHelper.stopPolling();
  jest.runAllTimers();

  expect(getIndexationStatus).not.toHaveBeenCalled();
});

it('should properly handle the flag to show the completed banner', () => {
  IndexationNotificationHelper.markCompletedNotificationAsToDisplay();

  expect(save).toHaveBeenCalledWith(expect.any(String), 'true');

  jest.mocked(get).mockReturnValueOnce('true');
  let shouldDisplay = IndexationNotificationHelper.shouldDisplayCompletedNotification();

  expect(shouldDisplay).toBe(true);
  expect(get).toHaveBeenCalled();

  IndexationNotificationHelper.markCompletedNotificationAsDisplayed();

  expect(remove).toHaveBeenCalled();

  shouldDisplay = IndexationNotificationHelper.shouldDisplayCompletedNotification();

  expect(shouldDisplay).toBe(false);
});
