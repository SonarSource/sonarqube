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
import { mockIssue } from '../../../../helpers/testMocks';
import SimilarIssuesPopup from '../SimilarIssuesPopup';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should render correctly when assigned', () => {
  expect(
    shallowRender({
      issue: mockIssue(false, { assignee: 'luke', assigneeName: 'Luke Skywalker' })
    }).find('SelectListItem[item="assignee"]')
  ).toMatchSnapshot();

  expect(
    shallowRender({ issue: mockIssue(false, { assignee: 'luke', assigneeActive: false }) }).find(
      'SelectListItem[item="assignee"]'
    )
  ).toMatchSnapshot();
});

it('should filter properly', () => {
  const issue = mockIssue();
  const onFilter = jest.fn();
  const wrapper = shallowRender({ issue, onFilter });
  wrapper.find('SelectList').prop<Function>('onSelect')('assignee');
  expect(onFilter).toBeCalledWith('assignee', issue);
});

function shallowRender(props: Partial<SimilarIssuesPopup['props']> = {}) {
  return shallow(
    <SimilarIssuesPopup
      issue={mockIssue(false, { subProject: 'foo', subProjectName: 'Foo', tags: ['test-tag'] })}
      onFilter={jest.fn()}
      {...props}
    />
  );
}
