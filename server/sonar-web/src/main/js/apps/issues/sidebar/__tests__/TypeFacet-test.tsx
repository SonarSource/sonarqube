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
import { click } from '../../../../helpers/testUtils';
import TypeFacet from '../TypeFacet';

it('should render open by default', () => {
  expect(shallowRender({ types: ['VULNERABILITY', 'CODE_SMELL'] })).toMatchSnapshot();
});

it('should toggle type facet', () => {
  const onToggle = jest.fn();
  const wrapper = shallowRender({ onToggle });
  click(wrapper.children('FacetHeader'));
  expect(onToggle).toHaveBeenCalledWith('types');
});

it('should clear types facet', () => {
  const onChange = jest.fn();
  const wrapper = shallowRender({ onChange, types: ['BUGS'] });
  wrapper.children('FacetHeader').prop<Function>('onClear')();
  expect(onChange).toHaveBeenCalledWith({ types: [] });
});

it('should select a type', () => {
  const onChange = jest.fn();
  const wrapper = shallowRender({ onChange });
  clickAndCheck('CODE_SMELL');
  clickAndCheck('VULNERABILITY', true, ['CODE_SMELL', 'VULNERABILITY']);

  function clickAndCheck(type: string, multiple = false, expected = [type]) {
    wrapper.find(`FacetItemsList`).find(`FacetItem[value="${type}"]`).prop<Function>('onClick')(
      type,
      multiple
    );
    expect(onChange).toHaveBeenLastCalledWith({ types: expected });
    wrapper.setProps({ types: expected });
  }
});

function shallowRender(props: Partial<TypeFacet['props']> = {}) {
  return shallow(
    <TypeFacet
      fetching={false}
      onChange={jest.fn()}
      onToggle={jest.fn()}
      stats={{ BUG: 0, VULNERABILITY: 2, CODE_SMELL: 5, SECURITY_HOTSPOT: 1 }}
      types={[]}
      {...props}
    />
  );
}
