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
import IssueChangelogDiff from '../IssueChangelogDiff';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(
    shallowRender({ diff: { key: 'file', oldValue: 'foo/bar.js', newValue: 'bar/baz.js' } })
  ).toMatchSnapshot();
  expect(
    shallowRender({ diff: { key: 'from_long_branch', oldValue: 'foo', newValue: 'bar' } })
  ).toMatchSnapshot();
  expect(
    shallowRender({ diff: { key: 'from_short_branch', oldValue: 'foo', newValue: 'bar' } })
  ).toMatchSnapshot();
  expect(shallowRender({ diff: { key: 'line', oldValue: '80' } })).toMatchSnapshot();
  expect(shallowRender({ diff: { key: 'effort', newValue: '12' } })).toMatchSnapshot();
  expect(
    shallowRender({ diff: { key: 'effort', newValue: '12', oldValue: '10' } })
  ).toMatchSnapshot();
  expect(shallowRender({ diff: { key: 'effort', oldValue: '10' } })).toMatchSnapshot();
});

function shallowRender(props: Partial<{ diff: T.IssueChangelogDiff }> = {}) {
  return shallow(<IssueChangelogDiff diff={{ key: 'foo' }} {...props} />);
}
