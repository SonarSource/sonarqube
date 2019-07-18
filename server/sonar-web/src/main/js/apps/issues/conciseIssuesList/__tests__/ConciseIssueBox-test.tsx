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
import { click, waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { mockIssue } from '../../../../helpers/testMocks';
import ConciseIssueBox from '../ConciseIssueBox';

it('should render correctly', async () => {
  const onClick = jest.fn();
  const wrapper = shallowRender({ onClick });
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();

  const issue = mockIssue(true);
  expect(shallowRender({ issue })).toMatchSnapshot();
  click(wrapper.find('.concise-issue-box-message'));
  expect(onClick).toBeCalledWith(issue.key);
});

const shallowRender = (props: Partial<ConciseIssueBox['props']> = {}) => {
  return shallow(
    <ConciseIssueBox
      issue={mockIssue()}
      onClick={jest.fn()}
      onFlowSelect={jest.fn()}
      onLocationSelect={jest.fn()}
      scroll={jest.fn()}
      selected={true}
      selectedFlowIndex={0}
      selectedLocationIndex={0}
      {...props}
    />
  );
};
