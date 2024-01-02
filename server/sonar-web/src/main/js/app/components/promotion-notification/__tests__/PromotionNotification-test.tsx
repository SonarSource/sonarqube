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
import { dismissNotice } from '../../../../api/users';
import { mockCurrentUser, mockLoggedInUser } from '../../../../helpers/testMocks';
import { waitAndUpdate } from '../../../../helpers/testUtils';
import { NoticeType } from '../../../../types/users';
import { PromotionNotification, PromotionNotificationProps } from '../PromotionNotification';

jest.mock('../../../../api/users', () => ({
  dismissNotice: jest.fn().mockResolvedValue({}),
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('anonymous');
  expect(
    shallowRender({
      currentUser: mockLoggedInUser({ dismissedNotices: { [NoticeType.SONARLINT_AD]: true } }),
    })
  ).toMatchSnapshot('adAlreadySeen');
  expect(shallowRender({ currentUser: mockLoggedInUser() })).toMatchSnapshot('loggedIn');
});

it('should remove the toaster when click on dismiss', async () => {
  const updateDismissedNotices = jest.fn();
  const wrapper = shallowRender({
    currentUser: mockLoggedInUser({ dismissedNotices: { [NoticeType.SONARLINT_AD]: false } }),
    updateDismissedNotices,
  });
  wrapper.find('.toaster-actions ButtonLink').simulate('click');
  expect(dismissNotice).toHaveBeenCalled();
  await waitAndUpdate(wrapper);
  expect(updateDismissedNotices).toHaveBeenCalled();
});

it('should remove the toaster and navigate to sonarlint when click on learn more', async () => {
  const updateDismissedNotices = jest.fn();
  const wrapper = shallowRender({
    currentUser: mockLoggedInUser({ dismissedNotices: { [NoticeType.SONARLINT_AD]: false } }),
    updateDismissedNotices,
  });
  wrapper.find('.toaster-actions .button-primary').simulate('click');
  expect(dismissNotice).toHaveBeenCalled();
  await waitAndUpdate(wrapper);
  expect(updateDismissedNotices).toHaveBeenCalled();
});

function shallowRender(props: Partial<PromotionNotificationProps> = {}) {
  return shallow(
    <PromotionNotification
      currentUser={mockCurrentUser()}
      updateDismissedNotices={jest.fn()}
      updateCurrentUserHomepage={jest.fn()}
      {...props}
    />
  );
}
