/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { KeyboardKeys } from '../../../../helpers/keycodes';
import { click, mockEvent } from '../../../../helpers/testUtils';
import CommentPopup, { CommentPopupProps } from '../CommentPopup';

it('should render the comment popup correctly without existing comment', () => {
  expect(shallowRender()).toMatchSnapshot();
});

it('should render the comment popup correctly when changing a comment', () => {
  expect(shallowRender({ comment: { markdown: '*test*' } })).toMatchSnapshot();
});

it('should render not allow to send comment with only spaces', () => {
  const onComment = jest.fn();
  const wrapper = shallowRender({ onComment });
  click(wrapper.find('.js-issue-comment-submit'));
  expect(onComment.mock.calls.length).toBe(0);
  wrapper.setState({ textComment: 'mycomment' });
  click(wrapper.find('.js-issue-comment-submit'));
  expect(onComment.mock.calls.length).toBe(1);
});

it('should render the alternative cancel button label', () => {
  const wrapper = shallowRender({ autoTriggered: true });
  expect(
    wrapper
      .find('.js-issue-comment-cancel')
      .childAt(0)
      .text()
  ).toBe('skip');
});

it('should handle ctrl+enter', () => {
  const onComment = jest.fn();
  const wrapper = shallowRender({ comment: { markdown: 'yes' }, onComment });

  wrapper
    .instance()
    .handleKeyboard(mockEvent({ ctrlKey: true, nativeEvent: { key: KeyboardKeys.Enter } }));

  expect(onComment).toBeCalled();
});

it('should stopPropagation for arrow keys events', () => {
  const wrapper = shallowRender();

  const event = mockEvent({
    nativeEvent: { key: KeyboardKeys.UpArrow },
    stopPropagation: jest.fn()
  });
  wrapper.instance().handleKeyboard(event);

  expect(event.stopPropagation).toBeCalled();
});

function shallowRender(overrides: Partial<CommentPopupProps> = {}) {
  return shallow<CommentPopup>(
    <CommentPopup
      onComment={jest.fn()}
      placeholder="placeholder test"
      toggleComment={jest.fn()}
      {...overrides}
    />
  );
}
