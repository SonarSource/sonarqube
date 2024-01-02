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
import { mockDefinition, mockSettingValue } from '../../../../helpers/mocks/settings';
import DefinitionRenderer, { DefinitionRendererProps } from '../DefinitionRenderer';

it('should render correctly', () => {
  expect(shallowRender({ loading: true })).toMatchSnapshot('loading');
  expect(
    shallowRender({ definition: mockDefinition({ description: 'description' }) })
  ).toMatchSnapshot('with description');
  expect(
    shallowRender({
      validationMessage: 'validation message',
    })
  ).toMatchSnapshot('in error');
  expect(shallowRender({ success: true })).toMatchSnapshot('success');
  expect(
    shallowRender({ settingValue: mockSettingValue({ key: 'foo', value: 'original value' }) })
  ).toMatchSnapshot('original value');

  expect(shallowRender({ changedValue: 'new value' })).toMatchSnapshot('changed value');
});

function shallowRender(props: Partial<DefinitionRendererProps> = {}) {
  return shallow<DefinitionRendererProps>(
    <DefinitionRenderer
      isEditing={false}
      definition={mockDefinition()}
      loading={false}
      onCancel={jest.fn()}
      onChange={jest.fn()}
      onEditing={jest.fn()}
      onReset={jest.fn()}
      onSave={jest.fn()}
      success={false}
      {...props}
    />
  );
}
