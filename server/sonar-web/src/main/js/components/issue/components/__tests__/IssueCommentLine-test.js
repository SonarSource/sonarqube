/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import React from 'react';
import IssueCommentLine from '../IssueCommentLine';
import { click } from '../../../../helpers/testUtils';

const comment = {
  key: 'comment-key',
  authorName: 'John Doe',
  authorAvatar: 'gravatarhash',
  htmlText: '<b>test</b>',
  createdAt: '2017-03-01T09:36:01+0100',
  updatable: true
};

jest.mock('moment', () => () => ({ fromNow: () => 'a month ago' }));

it('should render correctly a comment that is not updatable', () => {
  const element = shallow(
    <IssueCommentLine
      comment={{ ...comment, updatable: false }}
      onDelete={jest.fn()}
      onEdit={jest.fn()}
    />
  );
  expect(element).toMatchSnapshot();
});

it('should render correctly a comment that is updatable', () => {
  const element = shallow(
    <IssueCommentLine comment={comment} onDelete={jest.fn()} onEdit={jest.fn()} />
  );
  expect(element).toMatchSnapshot();
});

it('should open the right popups when the buttons are clicked', () => {
  const element = shallow(
    <IssueCommentLine comment={comment} onDelete={jest.fn()} onEdit={jest.fn()} />
  );
  click(element.find('button.js-issue-comment-edit'));
  expect(element.state()).toMatchSnapshot();
  click(element.find('button.js-issue-comment-delete'));
  expect(element.state()).toMatchSnapshot();
  expect(element).toMatchSnapshot();
});
