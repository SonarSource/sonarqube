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
import { shallow } from 'enzyme';
import * as React from 'react';
import {
  getNewCodePeriod,
  resetNewCodePeriod,
  setNewCodePeriod,
} from '../../../../api/newCodePeriod';
import { mockBranch, mockMainBranch, mockPullRequest } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockAppState } from '../../../../helpers/testMocks';
import { mockEvent, waitAndUpdate } from '../../../../helpers/testUtils';
import { App } from '../App';

jest.mock('../../../../api/newCodePeriod', () => ({
  getNewCodePeriod: jest.fn().mockResolvedValue({}),
  resetNewCodePeriod: jest.fn().mockResolvedValue({}),
  setNewCodePeriod: jest.fn().mockResolvedValue({}),
}));

it('should render correctly', async () => {
  let wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();

  wrapper = shallowRender({ appState: mockAppState({ canAdmin: true }), hasFeature: () => false });
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot('without branch support');
});

it('should initialize correctly', async () => {
  const wrapper = shallowRender({
    branchLikes: [mockBranch(), mockPullRequest(), mockMainBranch()],
  });
  await waitAndUpdate(wrapper);

  expect(wrapper.state().branchList).toHaveLength(2);
  expect(wrapper.state().referenceBranch).toBe('master');
});

it('should not display reset button if project setting is not set', () => {
  const wrapper = shallowRender();

  expect(wrapper.find('Button')).toHaveLength(0);
});

it('should reset the setting correctly', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  wrapper.instance().resetSetting();
  await waitAndUpdate(wrapper);
  expect(wrapper.state('currentSetting')).toBeUndefined();
  expect(wrapper.state('selected')).toBeUndefined();
});

it('should save correctly', async () => {
  const component = mockComponent();
  const wrapper = shallowRender({ component });
  await waitAndUpdate(wrapper);
  wrapper.setState({ selected: 'NUMBER_OF_DAYS', days: '23' });
  wrapper.instance().handleSubmit(mockEvent());
  await waitAndUpdate(wrapper);
  expect(setNewCodePeriod).toHaveBeenCalledWith({
    project: component.key,
    type: 'NUMBER_OF_DAYS',
    value: '23',
  });
  expect(wrapper.state('currentSetting')).toEqual(wrapper.state('selected'));
});

it('should handle errors gracefully', async () => {
  (getNewCodePeriod as jest.Mock).mockRejectedValue('error');
  (setNewCodePeriod as jest.Mock).mockRejectedValue('error');
  (resetNewCodePeriod as jest.Mock).mockRejectedValue('error');

  const wrapper = shallowRender();
  wrapper.setState({ selected: 'PREVIOUS_VERSION' });
  await waitAndUpdate(wrapper);

  expect(wrapper.state('loading')).toBe(false);
  wrapper.instance().resetSetting();
  await waitAndUpdate(wrapper);
  expect(wrapper.state('saving')).toBe(false);
  wrapper.instance().handleSubmit(mockEvent());
  await waitAndUpdate(wrapper);
  expect(wrapper.state('saving')).toBe(false);
});

function shallowRender(props: Partial<App['props']> = {}) {
  return shallow<App>(
    <App
      branchLike={mockBranch()}
      branchLikes={[mockMainBranch()]}
      appState={mockAppState({ canAdmin: true })}
      hasFeature={jest.fn().mockReturnValue(true)}
      component={mockComponent()}
      {...props}
    />
  );
}
