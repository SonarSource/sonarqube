/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import SelectLegacy from '../../../../components/controls/SelectLegacy';
import { mockUserBase } from '../../../../helpers/mocks/users';
import QualityGatePermissionsAddModalRenderer, {
  QualityGatePermissionsAddModalRendererProps
} from '../QualityGatePermissionsAddModalRenderer';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ query: 'a' })).toMatchSnapshot('short query');
  expect(shallowRender({ selection: mockUserBase() })).toMatchSnapshot('selection');
  expect(shallowRender({ selection: mockUserBase(), submitting: true })).toMatchSnapshot(
    'submitting'
  );
  expect(
    shallowRender({ query: 'ab', searchResults: [mockUserBase(), { name: 'group name' }] })
  ).toMatchSnapshot('query and results');
});

it('should render options correctly', () => {
  const wrapper = shallowRender();

  const { optionRenderer = () => null } = wrapper.find(SelectLegacy).props();

  expect(optionRenderer({ avatar: 'A', name: 'name', login: 'login' })).toMatchSnapshot('user');
  expect(optionRenderer({ name: 'group name' })).toMatchSnapshot('group');
});

function shallowRender(overrides: Partial<QualityGatePermissionsAddModalRendererProps> = {}) {
  return shallow(
    <QualityGatePermissionsAddModalRenderer
      loading={false}
      onClose={jest.fn()}
      onInputChange={jest.fn()}
      onSelection={jest.fn()}
      onSubmit={jest.fn()}
      query=""
      searchResults={[]}
      submitting={false}
      {...overrides}
    />
  );
}
