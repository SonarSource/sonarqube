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
/* eslint-disable import/order */
import * as React from 'react';
import { shallow } from 'enzyme';
import DefinitionActions from '../DefinitionActions';

const definition: T.SettingCategoryDefinition = {
  category: 'baz',
  description: 'lorem',
  fields: [],
  key: 'key',
  name: 'foobar',
  options: [],
  subCategory: 'bar',
  type: 'STRING'
};

const settings = {
  key: 'key',
  definition,
  value: 'baz'
};

it('displays default message when value is default', () => {
  const wrapper = shallowRender('', false, true);
  expect(wrapper).toMatchSnapshot();
});

it('displays save button when it can be saved', () => {
  const wrapper = shallowRender('foo', false, true);
  expect(wrapper).toMatchSnapshot();
});

it('displays cancel button when value changed and no error', () => {
  const wrapper = shallowRender('foo', false, true);
  expect(wrapper).toMatchSnapshot();
});

it('displays cancel button when value changed and has error', () => {
  const wrapper = shallowRender('foo', true, true);
  expect(wrapper).toMatchSnapshot();
});

it('disables save button on error', () => {
  const wrapper = shallowRender('foo', true, true);
  expect(wrapper).toMatchSnapshot();
});

it('displays reset button when empty and not default', () => {
  const wrapper = shallowRender('', true, false);
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(changedValue: string, hasError: boolean, isDefault: boolean) {
  return shallow(
    <DefinitionActions
      changedValue={changedValue}
      hasError={hasError}
      hasValueChanged={changedValue !== ''}
      isDefault={isDefault}
      onCancel={() => {}}
      onReset={() => {}}
      onSave={() => {}}
      setting={settings}
    />
  );
}
