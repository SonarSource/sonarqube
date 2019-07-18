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
import IssueCommentLine from '../IssueCommentLine';

const comment: T.IssueComment = {
  author: 'john.doe',
  authorActive: true,
  authorAvatar: 'gravatarhash',
  authorName: 'John Doe',
  createdAt: '2017-03-01T09:36:01+0100',
  htmlText: '<b>test</b>',
  key: 'comment-key',
  markdown: '*test*',
  updatable: true
};

it('should render correctly a comment that is updatable', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should render correctly a comment that is not updatable', () => {
  expect(shallowRender({ comment: { ...comment, updatable: false } })).toMatchSnapshot();
});

it('should open the right popups when the buttons are clicked', () => {
  const wrapper = shallowRender();
  click(wrapper.find('.js-issue-comment-edit'));
  expect(wrapper.state()).toMatchSnapshot();
  click(wrapper.find('.js-issue-comment-delete'));
  expect(wrapper.state()).toMatchSnapshot();
  wrapper.update();
  expect(wrapper).toMatchSnapshot();
});

it('should render correctly a comment with a deleted author', () => {
  expect(
    shallowRender({
      comment: { ...comment, authorActive: false, authorName: undefined }
    }).find('.issue-comment-author')
  ).toMatchSnapshot();
});

function shallowRender(props: Partial<IssueCommentLine['props']> = {}) {
  return shallow(
    <IssueCommentLine comment={comment} onDelete={jest.fn()} onEdit={jest.fn()} {...props} />
  );
}
