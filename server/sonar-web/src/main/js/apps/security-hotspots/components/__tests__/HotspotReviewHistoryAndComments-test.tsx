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
import {
  commentSecurityHotspot,
  deleteSecurityHotspotComment,
  editSecurityHotspotComment,
} from '../../../../api/security-hotspots';
import { mockHotspot } from '../../../../helpers/mocks/security-hotspots';
import { mockCurrentUser } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { isLoggedIn } from '../../../../types/users';
import HotspotReviewHistory from '../HotspotReviewHistory';
import HotspotReviewHistoryAndComments from '../HotspotReviewHistoryAndComments';

jest.mock('../../../../api/security-hotspots', () => ({
  commentSecurityHotspot: jest.fn().mockResolvedValue({}),
  deleteSecurityHotspotComment: jest.fn().mockResolvedValue({}),
  editSecurityHotspotComment: jest.fn().mockResolvedValue({}),
}));

jest.mock('../../../../types/users', () => ({ isLoggedIn: jest.fn(() => true) }));

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

it('should render correctly without user', () => {
  (isLoggedIn as any as jest.Mock<boolean, [boolean]>).mockReturnValueOnce(false);
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

it('should submit comment', async () => {
  const mockApi = commentSecurityHotspot as jest.Mock;
  const hotspot = mockHotspot();
  const wrapper = shallowRender({ hotspot });
  mockApi.mockClear();
  wrapper.instance().setState({ comment: 'Comment' });

  wrapper.find('#hotspot-comment-box-submit').simulate('click');
  await waitAndUpdate(wrapper);

  expect(mockApi).toHaveBeenCalledWith(hotspot.key, 'Comment');
  expect(wrapper.state().comment).toBe('');
  expect(wrapper.instance().props.onCommentUpdate).toHaveBeenCalledTimes(1);
});

it('should change comment', () => {
  const wrapper = shallowRender();
  wrapper.instance().setState({ comment: 'Comment' });
  wrapper.find('textarea').simulate('change', { target: { value: 'Foo' } });

  expect(wrapper.state().comment).toBe('Foo');
});

it('should reset on change hotspot', () => {
  const wrapper = shallowRender();
  wrapper.setState({ comment: 'NOP' });
  wrapper.setProps({ hotspot: mockHotspot({ key: 'other-hotspot' }) });

  expect(wrapper.state().comment).toBe('');
});

it('should delete comment', async () => {
  const wrapper = shallowRender();

  wrapper.find(HotspotReviewHistory).simulate('deleteComment', 'me1');
  await waitAndUpdate(wrapper);

  expect(deleteSecurityHotspotComment).toHaveBeenCalledWith('me1');
  expect(wrapper.instance().props.onCommentUpdate).toHaveBeenCalledTimes(1);
});

it('should edit comment', async () => {
  const wrapper = shallowRender();

  wrapper.find(HotspotReviewHistory).simulate('editComment', 'me1', 'new');
  await waitAndUpdate(wrapper);

  expect(editSecurityHotspotComment).toHaveBeenCalledWith('me1', 'new');
  expect(wrapper.instance().props.onCommentUpdate).toHaveBeenCalledTimes(1);
});

it('should correctly toggle the show full history state', () => {
  const wrapper = shallowRender();
  expect(wrapper.state().showFullHistory).toBe(false);
  wrapper.find(HotspotReviewHistory).props().onShowFullHistory();
  expect(wrapper.state().showFullHistory).toBe(true);
});

function shallowRender(props?: Partial<HotspotReviewHistoryAndComments['props']>) {
  return shallow<HotspotReviewHistoryAndComments>(
    <HotspotReviewHistoryAndComments
      commentTextRef={React.createRef()}
      currentUser={mockCurrentUser()}
      hotspot={mockHotspot()}
      onCommentUpdate={jest.fn()}
      {...props}
    />
  );
}
