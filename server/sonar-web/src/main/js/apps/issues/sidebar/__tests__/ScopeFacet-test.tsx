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
import { shallow, ShallowWrapper } from 'enzyme';
import * as React from 'react';
import FacetHeader from '../../../../components/facet/FacetHeader';
import FacetItem from '../../../../components/facet/FacetItem';
import { IssueScope } from '../../../../types/issues';
import ScopeFacet, { ScopeFacetProps } from '../ScopeFacet';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ open: true })).toMatchSnapshot('open');
  expect(shallowRender({ open: true, scopes: [IssueScope.Main] })).toMatchSnapshot('active facet');
  expect(shallowRender({ open: true, stats: { [IssueScope.Main]: 0 } })).toMatchSnapshot(
    'disabled facet'
  );
});

it('should correctly handle facet header clicks', () => {
  const onChange = jest.fn();
  const onToggle = jest.fn();
  const wrapper = shallowRender({ onChange, onToggle });

  wrapper.find(FacetHeader).props().onClear!();
  expect(onChange).toHaveBeenCalledWith({ scopes: [] });

  wrapper.find(FacetHeader).props().onClick!();
  expect(onToggle).toHaveBeenCalledWith('scopes');
});

it('should correctly handle facet item clicks', () => {
  const wrapper = shallowRender({ open: true, scopes: [IssueScope.Main] });
  const onChange = jest.fn(({ scopes }) => wrapper.setProps({ scopes }));
  wrapper.setProps({ onChange });

  clickFacetItem(wrapper, IssueScope.Test);
  expect(onChange).toHaveBeenLastCalledWith({ scopes: [IssueScope.Test] });

  clickFacetItem(wrapper, IssueScope.Test);
  expect(onChange).toHaveBeenLastCalledWith({ scopes: [] });

  clickFacetItem(wrapper, IssueScope.Test, true);
  clickFacetItem(wrapper, IssueScope.Main, true);
  expect(onChange).toHaveBeenLastCalledWith({
    scopes: expect.arrayContaining([IssueScope.Main, IssueScope.Test]),
  });

  clickFacetItem(wrapper, IssueScope.Test, true);
  expect(onChange).toHaveBeenLastCalledWith({ scopes: [IssueScope.Main] });
});

function clickFacetItem(
  wrapper: ShallowWrapper<ScopeFacetProps>,
  scope: IssueScope,
  multiple = false
) {
  return wrapper
    .find(FacetItem)
    .filterWhere((f) => f.key() === scope)
    .props()
    .onClick(scope, multiple);
}

function shallowRender(props: Partial<ScopeFacetProps> = {}) {
  return shallow<ScopeFacetProps>(
    <ScopeFacet
      fetching={true}
      onChange={jest.fn()}
      onToggle={jest.fn()}
      open={false}
      scopes={[]}
      stats={{}}
      {...props}
    />
  );
}
