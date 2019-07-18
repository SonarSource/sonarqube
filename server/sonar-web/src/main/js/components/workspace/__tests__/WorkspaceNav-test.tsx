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
import WorkspaceNav, { Props } from '../WorkspaceNav';

it('should render', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should not render open component', () => {
  expect(shallowRender({ open: { component: 'bar' } })).toMatchSnapshot();
});

it('should not render open rule', () => {
  expect(shallowRender({ open: { rule: 'qux' } })).toMatchSnapshot();
});

function shallowRender(props?: Partial<Props>) {
  const components = [{ branchLike: undefined, key: 'foo' }, { branchLike: undefined, key: 'bar' }];
  const rules = [{ key: 'qux', organization: 'org' }];
  return shallow(
    <WorkspaceNav
      components={components}
      onComponentClose={jest.fn()}
      onComponentOpen={jest.fn()}
      onRuleClose={jest.fn()}
      onRuleOpen={jest.fn()}
      open={{}}
      rules={rules}
      {...props}
    />
  );
}
