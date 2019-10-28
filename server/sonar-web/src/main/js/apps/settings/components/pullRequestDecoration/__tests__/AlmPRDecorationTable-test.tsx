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
import { ALM_KEYS } from '../../../../../types/alm-settings';
import AlmPRDecorationTable, { AlmPRDecorationTableProps } from '../AlmPRDecorationTable';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(
    shallowRender({
      additionalColumnsHeaders: ['additional1', 'additional2'],
      alm: ALM_KEYS.GITHUB,
      definitions: [
        { key: 'definition1', additionalColumns: ['def1-v1', 'def1-v2'] },
        { key: 'definition2', additionalColumns: ['def2-v1', 'def2-v2'] }
      ]
    })
  ).toMatchSnapshot();
});

it('should callback', () => {
  const onDelete = jest.fn();
  const onEdit = jest.fn();

  const wrapper = shallowRender({
    additionalColumnsHeaders: [],
    alm: ALM_KEYS.BITBUCKET,
    definitions: [{ key: 'defKey', additionalColumns: [] }],
    onDelete,
    onEdit
  });

  wrapper.find('DeleteButton').simulate('click');
  expect(onDelete).toBeCalledWith('defKey');

  wrapper.find('ButtonIcon').simulate('click');
  expect(onEdit).toBeCalledWith('defKey');
});

function shallowRender(props: Partial<AlmPRDecorationTableProps> = {}) {
  return shallow(
    <AlmPRDecorationTable
      additionalColumnsHeaders={[]}
      alm={ALM_KEYS.AZURE}
      definitions={[]}
      onDelete={jest.fn()}
      onEdit={jest.fn()}
      {...props}
    />
  );
}
