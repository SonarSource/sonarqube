/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { IndexationStatus } from '../../../../types/indexation';
import { IndexationContext } from '../IndexationContext';
import { IndexationContextProvider } from '../IndexationContextProvider';
import IndexationNotificationHelper from '../IndexationNotificationHelper';

beforeEach(() => jest.clearAllMocks());

jest.mock('../IndexationNotificationHelper');

it('should render correctly & start polling', () => {
  const wrapper = mountRender();

  expect(wrapper.state().status).toEqual({ isCompleted: false });

  const child = wrapper.find(TestComponent);
  expect(child.exists()).toBe(true);
  expect(child.instance().context).toEqual(wrapper.state());
});

it('should start polling if needed', () => {
  mountRender();

  expect(IndexationNotificationHelper.startPolling).toHaveBeenCalled();
});

it('should not start polling if not needed', () => {
  mountRender({ appState: { needIssueSync: false } });

  expect(IndexationNotificationHelper.startPolling).not.toHaveBeenCalled();
});

it('should update the state on new status & stop polling if indexation is complete', () => {
  const wrapper = mountRender();

  const triggerNewStatus = (IndexationNotificationHelper.startPolling as jest.Mock).mock
    .calls[0][0] as (status: IndexationStatus) => void;
  const newStatus = { isCompleted: true, percentCompleted: 100 };

  triggerNewStatus(newStatus);

  expect(wrapper.state().status).toEqual(newStatus);
  expect(IndexationNotificationHelper.stopPolling).toHaveBeenCalled();
});

it('should stop polling when component is destroyed', () => {
  const wrapper = mountRender();

  wrapper.unmount();

  expect(IndexationNotificationHelper.stopPolling).toHaveBeenCalled();
});

function mountRender(props?: IndexationContextProvider['props']) {
  return mount<IndexationContextProvider>(
    <IndexationContextProvider appState={{ needIssueSync: true }} {...props}>
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
