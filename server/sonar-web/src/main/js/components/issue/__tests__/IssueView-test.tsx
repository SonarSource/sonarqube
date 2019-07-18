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
import { mockIssue } from '../../../helpers/testMocks';
import IssueView from '../IssueView';

it('should render issues correctly', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should render hotspots correctly', () => {
  expect(
    shallowRender({ issue: mockIssue(false, { type: 'SECURITY_HOTSPOT' }) })
  ).toMatchSnapshot();
});

function shallowRender(props: Partial<IssueView['props']> = {}) {
  return shallow(
    <IssueView
      issue={mockIssue(false, {
        comments: [
          {
            key: '1',
            htmlText: 'My comment',
            markdown: 'My comment',
            updatable: false,
            createdAt: '2017-07-05T09:33:29+0200',
            author: 'admin',
            authorLogin: 'admin',
            authorName: 'Admin',
            authorAvatar: 'admin-avatar',
            authorActive: true
          }
        ]
      })}
      onAssign={jest.fn()}
      onChange={jest.fn()}
      onClick={jest.fn()}
      selected={true}
      togglePopup={jest.fn()}
      {...props}
    />
  );
}
