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
import { mount } from 'enzyme';
import * as React from 'react';
import { mockAppState } from '../../../../helpers/testMocks';
import { IndexationStatus } from '../../../../types/indexation';
import { IndexationContext } from '../IndexationContext';
import {
  IndexationContextProvider,
  IndexationContextProviderProps,
} from '../IndexationContextProvider';
import IndexationNotificationHelper from '../IndexationNotificationHelper';

beforeEach(() => jest.clearAllMocks());

jest.mock('../IndexationNotificationHelper');

it('should render correctly and start polling if issue sync is needed', () => {
  const wrapper = mountRender();

  expect(wrapper).toMatchSnapshot();
  expect(IndexationNotificationHelper.startPolling).toHaveBeenCalled();
});

it('should not start polling if no issue sync is needed', () => {
  const appState = mockAppState({ needIssueSync: false });
  const wrapper = mountRender({ appState });

  expect(IndexationNotificationHelper.startPolling).not.toHaveBeenCalled();

  const expectedStatus: IndexationStatus = {
    isCompleted: true,
    percentCompleted: 100,
    hasFailures: false,
  };
  expect(wrapper.state().status).toEqual(expectedStatus);
});

it('should update the state on new status', () => {
  const wrapper = mountRender();

  const triggerNewStatus = (IndexationNotificationHelper.startPolling as jest.Mock).mock
    .calls[0][0] as (status: IndexationStatus) => void;
  const newStatus: IndexationStatus = {
    isCompleted: true,
    percentCompleted: 100,
    hasFailures: false,
  };

  triggerNewStatus(newStatus);

  expect(wrapper.state().status).toEqual(newStatus);
});

it('should stop polling when component is destroyed', () => {
  const wrapper = mountRender();

  wrapper.unmount();

  expect(IndexationNotificationHelper.stopPolling).toHaveBeenCalled();
});

function mountRender(props?: IndexationContextProviderProps) {
  return mount<IndexationContextProvider>(
    <IndexationContextProvider appState={mockAppState({ needIssueSync: true, ...props?.appState })}>
      <TestComponent />
    </IndexationContextProvider>
  );
}

class TestComponent extends React.PureComponent {
  context!: IndexationStatus;
  static contextType = IndexationContext;

  render() {
    return <h1>TestComponent</h1>;
  }
}
