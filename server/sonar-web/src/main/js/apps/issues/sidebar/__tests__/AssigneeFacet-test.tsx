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
import * as React from 'react';
import { shallow } from 'enzyme';
import AssigneeFacet, { Props } from '../AssigneeFacet';
import { Query } from '../../utils';

jest.mock('../../../../store/rootReducer', () => ({}));

it('should render', () => {
  expect(renderAssigneeFacet({ assignees: ['foo'] })).toMatchSnapshot();
});

it('should select unassigned', () => {
  expect(
    renderAssigneeFacet({ assigned: false })
      .find('ListStyleFacet')
      .prop('values')
  ).toEqual(['']);
});

it('should call onChange', () => {
  const onChange = jest.fn();
  const wrapper = renderAssigneeFacet({ assignees: ['foo'], onChange });
  const itemOnClick = wrapper.find('ListStyleFacet').prop<Function>('onItemClick');

  itemOnClick('');
  expect(onChange).lastCalledWith({ assigned: false, assignees: [] });

  itemOnClick('bar');
  expect(onChange).lastCalledWith({ assigned: true, assignees: ['bar'] });

  itemOnClick('baz', true);
  expect(onChange).lastCalledWith({ assigned: true, assignees: ['baz', 'foo'] });
});

function renderAssigneeFacet(props?: Partial<Props>) {
  return shallow(
    <AssigneeFacet
      assigned={true}
      assignees={[]}
      fetching={false}
      loadSearchResultCount={jest.fn()}
      onChange={jest.fn()}
      onToggle={jest.fn()}
      open={true}
      organization={undefined}
      query={{} as Query}
      referencedUsers={{ foo: { avatar: 'avatart-foo', name: 'name-foo' } }}
      stats={{ '': 5, foo: 13, bar: 7, baz: 6 }}
      {...props}
    />
  );
}
