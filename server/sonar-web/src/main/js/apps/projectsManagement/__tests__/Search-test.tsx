/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as React from 'react';
import { shallow } from 'enzyme';
import Search, { Props } from '../Search';
import { click } from '../../../helpers/testUtils';
import { Visibility } from '../../../app/types';

const organization = { key: 'org', name: 'org', projectVisibility: Visibility.Public };

it('renders', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('render qualifiers filter', () => {
  expect(shallowRender({ topLevelQualifiers: ['TRK', 'VW', 'APP'] })).toMatchSnapshot();
});

it('updates qualifier', () => {
  const onQualifierChanged = jest.fn();
  const wrapper = shallowRender({ onQualifierChanged, topLevelQualifiers: ['TRK', 'VW', 'APP'] });
  wrapper.find('Select[name="projects-qualifier"]').prop<Function>('onChange')({ value: 'VW' });
  expect(onQualifierChanged).toBeCalledWith('VW');
});

it('selects provisioned', () => {
  const onProvisionedChanged = jest.fn();
  const wrapper = shallowRender({ onProvisionedChanged });
  wrapper.find('Checkbox[id="projects-provisioned"]').prop<Function>('onCheck')(true);
  expect(onProvisionedChanged).toBeCalledWith(true);
});

it('does not render provisioned filter for portfolios', () => {
  const wrapper = shallowRender();
  expect(wrapper.find('Checkbox[id="projects-provisioned"]').exists()).toBeTruthy();
  wrapper.setProps({ qualifiers: 'VW' });
  expect(wrapper.find('Checkbox[id="projects-provisioned"]').exists()).toBeFalsy();
});

it('updates analysis date', () => {
  const onDateChanged = jest.fn();
  const wrapper = shallowRender({ onDateChanged });

  wrapper.find('DateInput').prop<Function>('onChange')('2017-04-08T00:00:00.000Z');
  expect(onDateChanged).toBeCalledWith('2017-04-08T00:00:00.000Z');

  wrapper.find('DateInput').prop<Function>('onChange')(undefined);
  expect(onDateChanged).toBeCalledWith(undefined);
});

it('searches', () => {
  const onSearch = jest.fn();
  const wrapper = shallowRender({ onSearch });
  wrapper.find('SearchBox').prop<Function>('onChange')('foo');
  expect(onSearch).toBeCalledWith('foo');
});

it('checks all or none projects', () => {
  const onAllDeselected = jest.fn();
  const onAllSelected = jest.fn();
  const wrapper = shallowRender({ onAllDeselected, onAllSelected });

  wrapper.find('Checkbox[id="projects-selection"]').prop<Function>('onCheck')(true);
  expect(onAllSelected).toBeCalled();

  wrapper.find('Checkbox[id="projects-selection"]').prop<Function>('onCheck')(false);
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
  wrapper.update();
  expect(wrapper.find('BulkApplyTemplateModal').exists()).toBeFalsy();
});

function shallowRender(props?: { [P in keyof Props]?: Props[P] }) {
  return shallow(
    <Search
      onAllDeselected={jest.fn()}
      onAllSelected={jest.fn()}
      onDateChanged={jest.fn()}
      onDeleteProjects={jest.fn()}
      onProvisionedChanged={jest.fn()}
      onQualifierChanged={jest.fn()}
      onSearch={jest.fn()}
      organization={organization}
      projects={[]}
      provisioned={false}
      qualifiers="TRK"
      query=""
      ready={true}
      selection={[]}
      topLevelQualifiers={['TRK']}
      total={17}
      {...props}
    />
  );
}
