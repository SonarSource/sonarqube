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
import { mockIssue } from '../../../../helpers/testMocks';
import { click } from '../../../../helpers/testUtils';
import IssueAssign from '../IssueAssign';

const issue = mockIssue(false, {
  assignee: 'john',
  assigneeAvatar: 'gravatarhash',
  assigneeName: 'John Doe',
});

it('should render without the action when the correct rights are missing', () => {
  expect(shallowRender({ canAssign: false })).toMatchSnapshot();
});

it('should render with the action', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should render a fallback assignee display if assignee info are not available', () => {
  expect(
    shallowRender({ issue: mockIssue(false, { assignee: undefined, assigneeName: undefined }) })
  ).toMatchSnapshot();
});

it('should open the popup when the button is clicked', () => {
  const togglePopup = jest.fn();
  const element = shallowRender({ togglePopup });
  click(element.find('ButtonLink'));
  expect(togglePopup.mock.calls).toMatchSnapshot();
  element.setProps({ isOpen: true });
  expect(element).toMatchSnapshot();
});

function shallowRender(props: Partial<IssueAssign['props']> = {}) {
  return shallow<IssueAssign>(
    <IssueAssign
      canAssign={true}
      isOpen={false}
      issue={issue}
      onAssign={jest.fn()}
      togglePopup={jest.fn()}
      {...props}
    />
  );
}
