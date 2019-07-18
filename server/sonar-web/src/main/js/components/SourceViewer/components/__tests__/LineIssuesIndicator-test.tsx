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
import { click } from 'sonar-ui-common/helpers/testUtils';
import { mockIssue } from '../../../../helpers/testMocks';
import LineIssuesIndicator from '../LineIssuesIndicator';

it('should render correctly', () => {
  const onClick = jest.fn();
  const wrapper = shallowRender({ onClick });
  expect(wrapper).toMatchSnapshot();

  click(wrapper);
  expect(onClick).toHaveBeenCalled();

  const nextIssues = [
    mockIssue(false, { key: 'foo', type: 'VULNERABILITY' }),
    mockIssue(false, { key: 'bar', type: 'SECURITY_HOTSPOT' })
  ];
  wrapper.setProps({ issues: nextIssues });
  expect(wrapper).toMatchSnapshot();
});

it('should render correctly for no issues', () => {
  expect(shallowRender({ issues: [] })).toMatchSnapshot();
});

function shallowRender(props: Partial<LineIssuesIndicator['props']> = {}) {
  return shallow(
    <LineIssuesIndicator
      issues={[
        mockIssue(false, { key: 'foo', type: 'CODE_SMELL' }),
        mockIssue(false, { key: 'bar', type: 'BUG' })
      ]}
      line={{ line: 3 }}
      onClick={jest.fn()}
      {...props}
    />
  );
}
