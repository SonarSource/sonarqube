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
import { mockDefinition } from '../../../helpers/mocks/settings';
import { Button, ResetButtonLink } from '../../../sonar-ui-common/components/controls/buttons';
import Select from '../../../sonar-ui-common/components/controls/Select';
import ReportFrequencyForm, { ReportFrequencyFormProps } from '../ReportFrequencyForm';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ frequency: 'Weekly' })).toMatchSnapshot('changed');
  expect(shallowRender({ definition: mockDefinition() })).toMatchSnapshot('no description');
});

it('should handle changes', () => {
  const onSave = jest.fn();
  const wrapper = shallowRender({ onSave });

  wrapper.find(Select).simulate('change', { value: 'Daily' });

  expect(wrapper.find('.button-success').exists()).toBe(true);
  expect(wrapper.find(ResetButtonLink).exists()).toBe(true);

  wrapper.find(Button).simulate('click');

  expect(onSave).toBeCalledWith('Daily');
});

it('should handle reset', () => {
  const onSave = jest.fn();
  const wrapper = shallowRender({ frequency: 'Weekly', onSave });

  expect(wrapper.find('.button-success').exists()).toBe(false);
  wrapper.find(Button).simulate('click');

  expect(onSave).toBeCalledWith('Monthly');
});

function shallowRender(props: Partial<ReportFrequencyFormProps> = {}) {
  return shallow<ReportFrequencyFormProps>(
    <ReportFrequencyForm
      definition={mockDefinition({
        defaultValue: 'Monthly',
        description: 'description',
        options: ['Daily', 'Weekly', 'Monthly']
      })}
      frequency="Monthly"
      onSave={jest.fn()}
      {...props}
    />
  );
}
