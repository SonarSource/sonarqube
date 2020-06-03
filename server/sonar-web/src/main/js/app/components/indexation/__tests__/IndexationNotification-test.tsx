/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { IndexationNotification, IndexationProgression } from '../IndexationNotification';
import IndexationNotificationHelper from '../IndexationNotificationHelper';
import IndexationNotificationRenderer from '../IndexationNotificationRenderer';

beforeEach(() => jest.clearAllMocks());

jest.mock('../IndexationNotificationHelper');

it('should display the warning banner if indexation is in progress', () => {
  const wrapper = shallowRender();

  expect(IndexationNotificationHelper.markInProgressNotificationAsDisplayed).toHaveBeenCalled();
  expect(wrapper.state().progression).toBe(IndexationProgression.InProgress);
});

it('should display the success banner when indexation is complete', () => {
  (IndexationNotificationHelper.shouldDisplayCompletedNotification as jest.Mock).mockReturnValueOnce(
    true
  );

  const wrapper = shallowRender();

  wrapper.setProps({ indexationContext: { status: { isCompleted: true } } });

  expect(IndexationNotificationHelper.shouldDisplayCompletedNotification).toHaveBeenCalled();
  expect(wrapper.state().progression).toBe(IndexationProgression.Completed);
});

it('should render correctly completed notification at startup', () => {
  (IndexationNotificationHelper.shouldDisplayCompletedNotification as jest.Mock).mockReturnValueOnce(
    true
  );

  const wrapper = shallowRender({
    indexationContext: { status: { isCompleted: true } }
  });

  expect(IndexationNotificationHelper.markInProgressNotificationAsDisplayed).not.toHaveBeenCalled();
  expect(IndexationNotificationHelper.shouldDisplayCompletedNotification).toHaveBeenCalled();
  expect(wrapper.state().progression).toBe(IndexationProgression.Completed);
});

it('should hide the success banner on dismiss action', () => {
  (IndexationNotificationHelper.shouldDisplayCompletedNotification as jest.Mock).mockReturnValueOnce(
    true
  );

  const wrapper = shallowRender({
    indexationContext: { status: { isCompleted: true } }
  });

  wrapper
    .find(IndexationNotificationRenderer)
    .props()
    .onDismissCompletedNotification();

  expect(IndexationNotificationHelper.markCompletedNotificationAsDisplayed).toHaveBeenCalled();
  expect(wrapper.state().progression).toBeUndefined();
});

function shallowRender(props?: Partial<IndexationNotification['props']>) {
  return shallow<IndexationNotification>(
    <IndexationNotification indexationContext={{ status: { isCompleted: false } }} {...props} />
  );
}
