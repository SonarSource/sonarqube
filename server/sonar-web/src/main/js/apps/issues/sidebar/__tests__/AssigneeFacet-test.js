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
import React from 'react';
import { shallow } from 'enzyme';
import AssigneeFacet from '../AssigneeFacet';

jest.mock('../../../../store/rootReducer', () => ({}));

const renderAssigneeFacet = (props /*: ?{} */) =>
  shallow(
    <AssigneeFacet
      assigned={true}
      assignees={[]}
      facetMode="count"
      onChange={jest.fn()}
      onToggle={jest.fn()}
      open={true}
      referencedUsers={{ foo: { avatar: 'avatart-foo', name: 'name-foo' } }}
      stats={{ '': 5, foo: 13, bar: 7 }}
      {...props}
    />
  );

it('should render', () => {
  expect(renderAssigneeFacet()).toMatchSnapshot();
});

it('should render without stats', () => {
  expect(renderAssigneeFacet({ stats: null })).toMatchSnapshot();
});

it('should select unassigned', () => {
  expect(renderAssigneeFacet({ assigned: false })).toMatchSnapshot();
});

it('should select user', () => {
  expect(renderAssigneeFacet({ assignees: ['foo'] })).toMatchSnapshot();
});

it('should render footer select option', () => {
  const wrapper = renderAssigneeFacet();
  expect(
    wrapper.instance().renderOption({ avatar: 'avatar-foo', label: 'name-foo' })
  ).toMatchSnapshot();
});

it('should call onChange', () => {
  const onChange = jest.fn();
  const wrapper = renderAssigneeFacet({ assignees: ['foo'], onChange });
  const itemOnClick = wrapper
    .find('FacetItem')
    .first()
    .prop('onClick');

  itemOnClick('');
  expect(onChange).lastCalledWith({ assigned: false, assignees: [] });

  itemOnClick('bar');
  expect(onChange).lastCalledWith({ assigned: true, assignees: ['bar', 'foo'] });

  itemOnClick('foo');
  expect(onChange).lastCalledWith({ assigned: true, assignees: [] });
});

it('should call onToggle', () => {
  const onToggle = jest.fn();
  const wrapper = renderAssigneeFacet({ onToggle });
  const headerOnClick = wrapper.find('FacetHeader').prop('onClick');

  headerOnClick();
  expect(onToggle).lastCalledWith('assignees');
});

it('should handle footer callbacks', () => {
  const onChange = jest.fn();
  const wrapper = renderAssigneeFacet({ assignees: ['foo'], onChange });
  const onSelect = wrapper.find('FacetFooter').prop('onSelect');

  onSelect('qux');
  expect(onChange).lastCalledWith({ assigned: true, assignees: ['foo', 'qux'] });
});
