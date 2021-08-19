/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { setSimpleSettingValue } from '../../../api/settings';
import { mockComponent } from '../../../helpers/testMocks';
import { waitAndUpdate } from '../../../helpers/testUtils';
import { SettingsKey } from '../../../types/settings';
import ApplicationSettingsApp from '../ApplicationSettingsApp';

jest.mock('../../../api/settings', () => {
  const { mockDefinition } = jest.requireActual('../../../helpers/mocks/settings');

  const definition = mockDefinition({
    key: 'sonar.governance.report.project.branch.frequency', // SettingsKey.ProjectReportFrequency
    defaultValue: 'Monthly',
    description: 'description',
    options: ['Daily', 'Weekly', 'Monthly']
  });

  return {
    getDefinitions: jest.fn().mockResolvedValue([definition]),
    getValues: jest.fn().mockResolvedValue([{ value: 'Monthly' }]),
    setSimpleSettingValue: jest.fn().mockResolvedValue(undefined)
  };
});

it('should render correctly', async () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot('loading');
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot('default');
});

it('should handle submission', async () => {
  const component = mockComponent({ key: 'app-key' });
  const wrapper = shallowRender({ component });
  await waitAndUpdate(wrapper);

  wrapper.instance().handleSubmit('Daily');

  expect(setSimpleSettingValue).toBeCalledWith({
    component: component.key,
    key: SettingsKey.ProjectReportFrequency,
    value: 'Daily'
  });
});

function shallowRender(props: Partial<ApplicationSettingsApp['props']> = {}) {
  return shallow<ApplicationSettingsApp>(
    <ApplicationSettingsApp component={mockComponent()} {...props} />
  );
}
