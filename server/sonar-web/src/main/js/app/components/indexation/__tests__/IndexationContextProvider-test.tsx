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
import * as React from 'react';
import { useContext } from 'react';
import { mockAppState } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { byText } from '../../../../helpers/testSelector';
import { IndexationStatus } from '../../../../types/indexation';
import { IndexationContext } from '../IndexationContext';
import {
  IndexationContextProvider,
  IndexationContextProviderProps,
} from '../IndexationContextProvider';
import IndexationNotificationHelper from '../IndexationNotificationHelper';

beforeEach(() => jest.clearAllMocks());

jest.mock('../IndexationNotificationHelper');

it('should render correctly, start polling if issue sync is needed and stop when unmounted', () => {
  const { unmount } = renderIndexationContextProvider();
  expect(IndexationNotificationHelper.startPolling).toHaveBeenCalled();
  unmount();
  expect(IndexationNotificationHelper.stopPolling).toHaveBeenCalled();
});

it('should not start polling if no issue sync is needed', () => {
  const appState = mockAppState({ needIssueSync: false });
  renderIndexationContextProvider({ appState });
  expect(IndexationNotificationHelper.startPolling).not.toHaveBeenCalled();
});

it('should update the state on new status', async () => {
  renderIndexationContextProvider();

  const triggerNewStatus = jest.mocked(IndexationNotificationHelper.startPolling).mock
    .calls[0][0] as (status: IndexationStatus) => void;

  const newStatus: IndexationStatus = {
    hasFailures: false,
    isCompleted: true,
  };

  expect(byText('null').get()).toBeInTheDocument();

  triggerNewStatus(newStatus);

  expect(
    await byText('{"status":{"hasFailures":false,"isCompleted":true}}').find(),
  ).toBeInTheDocument();
});

function renderIndexationContextProvider(props?: IndexationContextProviderProps) {
  return renderComponent(
    <IndexationContextProvider appState={mockAppState({ needIssueSync: true, ...props?.appState })}>
      <TestComponent />
    </IndexationContextProvider>,
  );
}

function TestComponent() {
  const state = useContext(IndexationContext);
  return <div>{JSON.stringify(state)}</div>;
}
