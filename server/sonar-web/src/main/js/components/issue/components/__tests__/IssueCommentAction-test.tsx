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
import IssueCommentAction from '../IssueCommentAction';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should open the popup when the button is clicked', () => {
  const toggleComment = jest.fn();
  const element = shallowRender({ toggleComment });
  click(element.find('ButtonLink'));
  expect(toggleComment.mock.calls.length).toBe(1);
  element.setProps({ currentPopup: 'comment' });
  expect(element).toMatchSnapshot();
});

function shallowRender(props: Partial<IssueCommentAction['props']> = {}) {
  return shallow(
    <IssueCommentAction
      commentPlaceholder=""
      issueKey="issue-key"
      onChange={jest.fn()}
      toggleComment={jest.fn()}
      {...props}
    />
  );
}
