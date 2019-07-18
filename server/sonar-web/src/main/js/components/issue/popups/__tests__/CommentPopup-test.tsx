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
import CommentPopup from '../CommentPopup';

it('should render the comment popup correctly without existing comment', () => {
  const element = shallow(
    <CommentPopup onComment={jest.fn()} placeholder="placeholder test" toggleComment={jest.fn()} />
  );
  expect(element).toMatchSnapshot();
});

it('should render the comment popup correctly when changing a comment', () => {
  const element = shallow(
    <CommentPopup
      comment={{ markdown: '*test*' }}
      onComment={jest.fn()}
      placeholder=""
      toggleComment={jest.fn()}
    />
  );
  expect(element).toMatchSnapshot();
});

it('should render not allow to send comment with only spaces', () => {
  const onComment = jest.fn();
  const element = shallow(
    <CommentPopup onComment={onComment} placeholder="placeholder test" toggleComment={jest.fn()} />
  );
  click(element.find('.js-issue-comment-submit'));
  expect(onComment.mock.calls.length).toBe(0);
  element.setState({ textComment: 'mycomment' });
  click(element.find('.js-issue-comment-submit'));
  expect(onComment.mock.calls.length).toBe(1);
});

it('should render the alternative cancel button label', () => {
  const element = shallow(
    <CommentPopup
      autoTriggered={true}
      onComment={jest.fn()}
      placeholder=""
      toggleComment={jest.fn()}
    />
  );
  expect(
    element
      .find('.js-issue-comment-cancel')
      .childAt(0)
      .text()
  ).toBe('skip');
});
