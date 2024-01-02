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
import { dismissAnalysisWarning, getTask } from '../../../api/ce';
import { mockTaskWarning } from '../../../helpers/mocks/tasks';
import { mockCurrentUser } from '../../../helpers/testMocks';
import { waitAndUpdate } from '../../../helpers/testUtils';
import { ButtonLink } from '../../controls/buttons';
import { AnalysisWarningsModal } from '../AnalysisWarningsModal';

jest.mock('../../../api/ce', () => ({
  dismissAnalysisWarning: jest.fn().mockResolvedValue(null),
  getTask: jest.fn().mockResolvedValue({
    warnings: ['message foo', 'message-bar', 'multiline message\nsecondline\n  third line'],
  }),
}));

beforeEach(jest.clearAllMocks);

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ warnings: [mockTaskWarning({ dismissable: true })] })).toMatchSnapshot(
    'with dismissable warnings'
  );
  expect(
    shallowRender({
      currentUser: mockCurrentUser({ isLoggedIn: false }),
      warnings: [mockTaskWarning({ dismissable: true })],
    })
  ).toMatchSnapshot('do not show dismissable links for anonymous');
});

it('should not fetch task warnings if it does not have to', () => {
  shallowRender();
  expect(getTask).not.toHaveBeenCalled();
});

it('should fetch task warnings if it has to', async () => {
  const wrapper = shallowRender({ taskId: 'abcd1234', warnings: undefined });
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(getTask).toHaveBeenCalledWith('abcd1234', ['warnings']);
});

it('should correctly handle dismissing warnings', async () => {
  const onWarningDismiss = jest.fn();
  const wrapper = shallowRender({
    componentKey: 'foo',
    onWarningDismiss,
    warnings: [mockTaskWarning({ key: 'bar', dismissable: true })],
  });

  const { onClick } = wrapper.find(ButtonLink).at(0).props();

  if (onClick) {
    onClick();
  }

  await waitAndUpdate(wrapper);

  expect(dismissAnalysisWarning).toHaveBeenCalledWith('foo', 'bar');
  expect(onWarningDismiss).toHaveBeenCalled();
});

it('should correctly handle updates', async () => {
  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);
  expect(getTask).not.toHaveBeenCalled();

  wrapper.setProps({ taskId: '1', warnings: undefined });
  await waitAndUpdate(wrapper);
  expect(getTask).toHaveBeenCalled();

  (getTask as jest.Mock).mockClear();
  wrapper.setProps({ taskId: undefined, warnings: [mockTaskWarning()] });
  expect(getTask).not.toHaveBeenCalled();
});

function shallowRender(props: Partial<AnalysisWarningsModal['props']> = {}) {
  return shallow<AnalysisWarningsModal>(
    <AnalysisWarningsModal
      currentUser={mockCurrentUser({ isLoggedIn: true })}
      onClose={jest.fn()}
      warnings={[
        mockTaskWarning({ message: 'warning 1' }),
        mockTaskWarning({ message: 'warning 2' }),
      ]}
      {...props}
    />
  );
}
