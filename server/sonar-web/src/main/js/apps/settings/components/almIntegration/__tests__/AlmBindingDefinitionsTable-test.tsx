/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { AlmKeys } from '../../../../../types/alm-settings';
import AlmBindingDefinitionsTable, {
  AlmBindingDefinitionsTableProps
} from '../AlmBindingDefinitionsTable';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(
    shallowRender({
      additionalColumnsHeaders: ['additional1', 'additional2'],
      alm: AlmKeys.GitHub,
      definitions: [
        { key: 'definition1', additionalColumns: ['def1-v1', 'def1-v2'] },
        { key: 'definition2', additionalColumns: ['def2-v1', 'def2-v2'] }
      ]
    })
  ).toMatchSnapshot('additional columns');
  expect(shallowRender({ alm: AlmKeys.GitLab })).toMatchSnapshot('title adjusts for GitLab');
});

it('should correctly trigger create, delete, and edit', () => {
  const onCreate = jest.fn();
  const onDelete = jest.fn();
  const onEdit = jest.fn();

  const wrapper = shallowRender({
    additionalColumnsHeaders: [],
    alm: AlmKeys.Bitbucket,
    definitions: [{ key: 'defKey', additionalColumns: [] }],
    onCreate,
    onDelete,
    onEdit
  });

  wrapper.find('Button').simulate('click');
  expect(onCreate).toBeCalled();

  wrapper.find('DeleteButton').simulate('click');
  expect(onDelete).toBeCalledWith('defKey');

  wrapper.find('ButtonIcon').simulate('click');
  expect(onEdit).toBeCalledWith('defKey');
});

function shallowRender(props: Partial<AlmBindingDefinitionsTableProps> = {}) {
  return shallow(
    <AlmBindingDefinitionsTable
      additionalColumnsHeaders={[]}
      alm={AlmKeys.Azure}
      definitions={[]}
      onCreate={jest.fn()}
      onDelete={jest.fn()}
      onEdit={jest.fn()}
      {...props}
    />
  );
}
