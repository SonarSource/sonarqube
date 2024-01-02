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
import React from 'react';
import { Button, EditButton } from '../../../../components/controls/buttons';
import Dropdown, { DropdownOverlay } from '../../../../components/controls/Dropdown';
import Toggler from '../../../../components/controls/Toggler';
import { mockIssueChangelog } from '../../../../helpers/mocks/issues';
import { mockHotspot, mockHotspotComment } from '../../../../helpers/mocks/security-hotspots';
import { mockUser } from '../../../../helpers/testMocks';
import HotspotCommentPopup from '../HotspotCommentPopup';
import HotspotReviewHistory, { HotspotReviewHistoryProps } from '../HotspotReviewHistory';

jest.mock('react', () => {
  return {
    ...jest.requireActual('react'),
    useState: jest.fn().mockImplementation(() => ['', jest.fn()]),
  };
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ showFullHistory: true })).toMatchSnapshot('show full list');
  expect(shallowRender({ showFullHistory: true }).find(Toggler).props().overlay).toMatchSnapshot(
    'edit comment overlay'
  );
  expect(shallowRender({ showFullHistory: true }).find(Dropdown).props().overlay).toMatchSnapshot(
    'delete comment overlay'
  );
});

it('should correctly handle comment updating', () => {
  return new Promise<void>((resolve, reject) => {
    const setEditedCommentKey = jest.fn();
    (React.useState as jest.Mock).mockImplementationOnce(() => ['', setEditedCommentKey]);

    const onEditComment = jest.fn();
    const wrapper = shallowRender({ onEditComment, showFullHistory: true });

    // Closing the Toggler sets the edited key back to an empty string.
    wrapper.find(Toggler).at(0).props().onRequestClose();
    expect(setEditedCommentKey).toHaveBeenCalledWith('');

    const editOnClick = wrapper.find(EditButton).at(0).props().onClick;
    if (!editOnClick) {
      reject();
      return;
    }

    // Clicking on the EditButton correctly flags the comment for editing.
    editOnClick();
    expect(setEditedCommentKey).toHaveBeenLastCalledWith('comment-1');

    // Cancelling an edit sets the edited key back to an empty string
    const dropdownOverlay = shallow(
      wrapper.find(Toggler).at(0).props().overlay as React.ReactElement<DropdownOverlay>
    );
    dropdownOverlay.find(HotspotCommentPopup).props().onCancelEdit();
    expect(setEditedCommentKey).toHaveBeenLastCalledWith('');

    // Updating the comment sets the edited key back to an empty string, and calls the
    // prop to update the comment value.
    dropdownOverlay.find(HotspotCommentPopup).props().onCommentEditSubmit('comment');
    expect(onEditComment).toHaveBeenLastCalledWith('comment-1', 'comment');
    expect(setEditedCommentKey).toHaveBeenLastCalledWith('');
    expect(setEditedCommentKey).toHaveBeenCalledTimes(4);

    resolve();
  });
});

it('should correctly handle comment deleting', () => {
  return new Promise<void>((resolve, reject) => {
    const setEditedCommentKey = jest.fn();
    (React.useState as jest.Mock).mockImplementationOnce(() => ['', setEditedCommentKey]);

    const onDeleteComment = jest.fn();
    const wrapper = shallowRender({ onDeleteComment, showFullHistory: true });

    // Opening the deletion Dropdown sets the edited key back to an empty string.
    const dropdownOnOpen = wrapper.find(Dropdown).at(0).props().onOpen;
    if (!dropdownOnOpen) {
      reject();
      return;
    }
    dropdownOnOpen();
    expect(setEditedCommentKey).toHaveBeenLastCalledWith('');

    // Confirming deletion calls the prop to delete the comment.
    const dropdownOverlay = shallow(
      wrapper.find(Dropdown).at(0).props().overlay as React.ReactElement<HTMLDivElement>
    );
    const deleteButtonOnClick = dropdownOverlay.find(Button).props().onClick;
    if (!deleteButtonOnClick) {
      reject();
      return;
    }

    deleteButtonOnClick();
    expect(onDeleteComment).toHaveBeenCalledWith('comment-1');

    resolve();
  });
});

function shallowRender(props?: Partial<HotspotReviewHistoryProps>) {
  return shallow(
    <HotspotReviewHistory
      hotspot={mockHotspot({
        creationDate: '2018-09-01',
        changelog: [
          mockIssueChangelog(),
          mockIssueChangelog({
            creationDate: '2018-10-12',
          }),
        ],
        comment: [
          mockHotspotComment({
            key: 'comment-1',
            updatable: true,
          }),
          mockHotspotComment({ key: 'comment-2', user: mockUser({ name: undefined }) }),
          mockHotspotComment({ key: 'comment-3', user: mockUser({ active: false }) }),
          mockHotspotComment({ key: 'comment-4' }),
          mockHotspotComment({ key: 'comment-5' }),
        ],
      })}
      onDeleteComment={jest.fn()}
      onEditComment={jest.fn()}
      onShowFullHistory={jest.fn()}
      showFullHistory={false}
      {...props}
    />
  );
}
