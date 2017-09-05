/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import * as React from 'react';
import { shallow } from 'enzyme';
import Search, { Props } from '../Search';
import { Type } from '../utils';
import { change, click } from '../../../helpers/testUtils';

const organization = { key: 'org', name: 'org', projectVisibility: 'public' };

it('renders', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('render qualifiers filter', () => {
  expect(shallowRender({ topLevelQualifiers: ['TRK', 'VW', 'APP'] })).toMatchSnapshot();
});

it('updates qualifier', () => {
  const onQualifierChanged = jest.fn();
  const wrapper = shallowRender({ onQualifierChanged, topLevelQualifiers: ['TRK', 'VW', 'APP'] });
  wrapper.find('RadioToggle[name="projects-qualifier"]').prop<Function>('onCheck')('VW');
  expect(onQualifierChanged).toBeCalledWith('VW');
});

it('updates type', () => {
  const onTypeChanged = jest.fn();
  const wrapper = shallowRender({ onTypeChanged });
  wrapper.find('RadioToggle[name="projects-type"]').prop<Function>('onCheck')(Type.Provisioned);
  expect(onTypeChanged).toBeCalledWith(Type.Provisioned);
});

it('searches', () => {
  const onSearch = jest.fn();
  const wrapper = shallowRender({ onSearch });
  change(wrapper.find('input[type="search"]'), 'foo');
  expect(onSearch).toBeCalledWith('foo');
});

it('checks all or none projects', () => {
  const onAllDeselected = jest.fn();
  const onAllSelected = jest.fn();
  const wrapper = shallowRender({ onAllDeselected, onAllSelected });

  wrapper.find('Checkbox').prop<Function>('onCheck')(true);
  expect(onAllSelected).toBeCalled();

  wrapper.find('Checkbox').prop<Function>('onCheck')(false);
  expect(onAllDeselected).toBeCalled();
});

it('deletes projects', () => {
  const onDeleteProjects = jest.fn();
  const wrapper = shallowRender({ onDeleteProjects, selection: ['foo', 'bar'] });
  click(wrapper.find('.js-delete'));
  expect(wrapper.find('DeleteModal')).toMatchSnapshot();
  wrapper.find('DeleteModal').prop<Function>('onConfirm')();
  expect(onDeleteProjects).toBeCalled();
});

it('bulk applies permission template', () => {
  const wrapper = shallowRender({});
  click(wrapper.find('.js-bulk-apply-permission-template'));
  expect(wrapper.find('BulkApplyTemplateModal')).toMatchSnapshot();
  wrapper.find('BulkApplyTemplateModal').prop<Function>('onClose')();
  expect(wrapper.find('BulkApplyTemplateModal').exists()).toBeFalsy();
});

function shallowRender(props?: { [P in keyof Props]?: Props[P] }) {
  return shallow(
    <Search
      onAllDeselected={jest.fn()}
      onAllSelected={jest.fn()}
      onDeleteProjects={jest.fn()}
      onQualifierChanged={jest.fn()}
      onSearch={jest.fn()}
      onTypeChanged={jest.fn()}
      organization={organization}
      projects={[]}
      qualifiers="TRK"
      query=""
      ready={true}
      selection={[]}
      topLevelQualifiers={['TRK']}
      total={0}
      type={Type.All}
      {...props}
    />
  );
}
