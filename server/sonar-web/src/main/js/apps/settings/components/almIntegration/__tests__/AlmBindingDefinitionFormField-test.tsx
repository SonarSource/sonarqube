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
import { ButtonLink } from '../../../../../components/controls/buttons';
import { click } from '../../../../../helpers/testUtils';
import { AlmBindingDefinitionBase } from '../../../../../types/alm-settings';
import {
  AlmBindingDefinitionFormField,
  AlmBindingDefinitionFormFieldProps,
} from '../AlmBindingDefinitionFormField';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ help: 'help' })).toMatchSnapshot('with help');
  expect(shallowRender({ isTextArea: true })).toMatchSnapshot('textarea');
  expect(shallowRender({ optional: true })).toMatchSnapshot('optional');
  expect(shallowRender({ overwriteOnly: true })).toMatchSnapshot('secret');
  expect(shallowRender({ isSecret: true })).toMatchSnapshot('encryptable');
  expect(shallowRender({ error: 'some error message', isInvalid: true })).toMatchSnapshot(
    'invalid with error'
  );
});

it('should call onFieldChange', () => {
  const onInputChange = jest.fn();
  shallowRender({ onFieldChange: onInputChange })
    .find('input')
    .simulate('change', { currentTarget: { value: '' } });
  expect(onInputChange).toHaveBeenCalled();

  const onTextAreaChange = jest.fn();
  shallowRender({ isTextArea: true, onFieldChange: onTextAreaChange })
    .find('textarea')
    .simulate('change', { currentTarget: { value: '' } });
  expect(onTextAreaChange).toHaveBeenCalled();
});

it('should correctly toggle visibility for secret fields', () => {
  const onFieldChange = jest.fn();
  const wrapper = shallowRender({ onFieldChange, overwriteOnly: true });
  expect(wrapper.find('input').exists()).toBe(false);

  click(wrapper.find(ButtonLink));
  expect(onFieldChange).toHaveBeenCalledWith('key', '');
  expect(wrapper.find('input').exists()).toBe(true);
});

function shallowRender(
  props: Partial<AlmBindingDefinitionFormFieldProps<AlmBindingDefinitionBase>> = {}
) {
  return shallow(
    <AlmBindingDefinitionFormField
      id="key"
      isTextArea={false}
      maxLength={40}
      onFieldChange={jest.fn()}
      propKey="key"
      value="key"
      {...props}
    />
  );
}
