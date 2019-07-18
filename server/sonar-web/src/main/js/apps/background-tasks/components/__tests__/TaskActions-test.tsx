/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { click } from 'sonar-ui-common/helpers/testUtils';
import TaskActions from '../TaskActions';

it('renders', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ status: 'SUCCESS' })).toMatchSnapshot();
  expect(shallowRender({ hasScannerContext: true })).toMatchSnapshot();
  expect(shallowRender({ errorMessage: 'error!' })).toMatchSnapshot();
  expect(shallowRender({}, { component: { key: 'foo' } })).toMatchSnapshot();
});

it('shows stack trace', () => {
  const wrapper = shallowRender({ errorMessage: 'error!' });
  click(wrapper.find('.js-task-show-stacktrace'));
  expect(wrapper.find('Stacktrace')).toMatchSnapshot();
  wrapper.find('Stacktrace').prop<Function>('onClose')();
  wrapper.update();
  expect(wrapper.find('Stacktrace').exists()).toBeFalsy();
});

it('shows scanner context', () => {
  const wrapper = shallowRender({ hasScannerContext: true });
  click(wrapper.find('.js-task-show-scanner-context'));
  expect(wrapper.find('ScannerContext')).toMatchSnapshot();
  wrapper.find('ScannerContext').prop<Function>('onClose')();
  wrapper.update();
  expect(wrapper.find('ScannerContext').exists()).toBeFalsy();
});

it('shows warnings', () => {
  const wrapper = shallowRender({ warningCount: 2 });
  click(wrapper.find('.js-task-show-warnings'));
  expect(wrapper.find('AnalysisWarningsModal')).toMatchSnapshot();
  wrapper.find('AnalysisWarningsModal').prop<Function>('onClose')();
  wrapper.update();
  expect(wrapper.find('AnalysisWarningsModal').exists()).toBeFalsy();
});

function shallowRender(fields?: Partial<T.Task>, props?: Partial<TaskActions['props']>) {
  return shallow(
    <TaskActions
      onCancelTask={jest.fn()}
      onFilterTask={jest.fn()}
      task={{
        componentName: 'foo',
        status: 'PENDING',
        id: '123',
        organization: 'org',
        submittedAt: '2017-01-01',
        type: 'REPORT',
        ...fields
      }}
      {...props}
    />
  );
}
