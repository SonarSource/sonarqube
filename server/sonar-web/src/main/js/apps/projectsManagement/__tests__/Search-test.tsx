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
import { mockReactSelectOptionProps } from '../../../helpers/mocks/react-select';
import { mockAppState } from '../../../helpers/testMocks';
import { click } from '../../../helpers/testUtils';
import { Props, Search } from '../Search';

it('renders', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('disables the delete and bulk apply buttons unless a project is selected', () => {
  const wrapper = shallowRender();
  expect(wrapper.find('Button.js-delete').prop('disabled')).toBe(true);
  expect(wrapper.find('Button.js-bulk-apply-permission-template').prop('disabled')).toBe(true);

  wrapper.setProps({ selection: ['foo'] });
  expect(wrapper.find('Button.js-delete').prop('disabled')).toBe(false);
  expect(wrapper.find('Button.js-bulk-apply-permission-template').prop('disabled')).toBe(false);
});

it('render qualifiers filter', () => {
  expect(
    shallowRender({ appState: mockAppState({ qualifiers: ['TRK', 'VW', 'APP'] }) })
  ).toMatchSnapshot();
});

it('updates qualifier', () => {
  const onQualifierChanged = jest.fn();
  const wrapper = shallowRender({
    onQualifierChanged,
    appState: mockAppState({ qualifiers: ['TRK', 'VW', 'APP'] }),
  });
  wrapper.find('Select[name="projects-qualifier"]').simulate('change', {
    value: 'VW',
  });
  expect(onQualifierChanged).toHaveBeenCalledWith('VW');
});

it('renders optionrenderer and singlevaluerenderer', () => {
  const wrapper = shallowRender({
    appState: mockAppState({ qualifiers: ['TRK', 'VW', 'APP'] }),
  });
  const OptionRendererer = wrapper.instance().optionRenderer;
  const SingleValueRendererer = wrapper.instance().singleValueRenderer;
  expect(
    shallow(<OptionRendererer {...mockReactSelectOptionProps({ value: 'val' })} />)
  ).toMatchSnapshot('option renderer');
  expect(
    shallow(<SingleValueRendererer {...mockReactSelectOptionProps({ value: 'val' })} />)
  ).toMatchSnapshot('single value renderer');
});

it('selects provisioned', () => {
  const onProvisionedChanged = jest.fn();
  const wrapper = shallowRender({ onProvisionedChanged });
  wrapper.find('Checkbox[id="projects-provisioned"]').prop<Function>('onCheck')(true);
  expect(onProvisionedChanged).toHaveBeenCalledWith(true);
});

it('does not render provisioned filter for portfolios', () => {
  const wrapper = shallowRender();
  expect(wrapper.find('Checkbox[id="projects-provisioned"]').exists()).toBe(true);
  wrapper.setProps({ qualifiers: 'VW' });
  expect(wrapper.find('Checkbox[id="projects-provisioned"]').exists()).toBe(false);
});

it('updates analysis date', () => {
  const onDateChanged = jest.fn();
  const wrapper = shallowRender({ onDateChanged });

  wrapper.find('DateInput').prop<Function>('onChange')('2017-04-08T00:00:00.000Z');
  expect(onDateChanged).toHaveBeenCalledWith('2017-04-08T00:00:00.000Z');

  wrapper.find('DateInput').prop<Function>('onChange')(undefined);
  expect(onDateChanged).toHaveBeenCalledWith(undefined);
});

it('searches', () => {
  const onSearch = jest.fn();
  const wrapper = shallowRender({ onSearch });
  wrapper.find('SearchBox').prop<Function>('onChange')('foo');
  expect(onSearch).toHaveBeenCalledWith('foo');
});

it('checks all or none projects', () => {
  const onAllDeselected = jest.fn();
  const onAllSelected = jest.fn();
  const wrapper = shallowRender({ onAllDeselected, onAllSelected });

  wrapper.find('Checkbox[id="projects-selection"]').prop<Function>('onCheck')(true);
  expect(onAllSelected).toHaveBeenCalled();

  wrapper.find('Checkbox[id="projects-selection"]').prop<Function>('onCheck')(false);
  expect(onAllDeselected).toHaveBeenCalled();
});

it('deletes projects', () => {
  const onDeleteProjects = jest.fn();
  const wrapper = shallowRender({ onDeleteProjects, selection: ['foo', 'bar'] });
  click(wrapper.find('.js-delete'));
  expect(wrapper.find('DeleteModal')).toMatchSnapshot();
  wrapper.find('DeleteModal').prop<Function>('onConfirm')();
  expect(onDeleteProjects).toHaveBeenCalled();
});

it('bulk applies permission template', () => {
  const wrapper = shallowRender({});
  click(wrapper.find('.js-bulk-apply-permission-template'));
  expect(wrapper.find('BulkApplyTemplateModal')).toMatchSnapshot();
  wrapper.find('BulkApplyTemplateModal').prop<Function>('onClose')();
  wrapper.update();
  expect(wrapper.find('BulkApplyTemplateModal').exists()).toBe(false);
});

function shallowRender(props?: { [P in keyof Props]?: Props[P] }) {
  return shallow<Search>(
    <Search
      analyzedBefore={undefined}
      onAllDeselected={jest.fn()}
      onAllSelected={jest.fn()}
      onDateChanged={jest.fn()}
      onDeleteProjects={jest.fn()}
      onProvisionedChanged={jest.fn()}
      onQualifierChanged={jest.fn()}
      onSearch={jest.fn()}
      onVisibilityChanged={jest.fn()}
      projects={[]}
      provisioned={false}
      qualifiers="TRK"
      query=""
      ready={true}
      selection={[]}
      appState={mockAppState({ qualifiers: ['TRK'] })}
      total={17}
      {...props}
    />
  );
}
