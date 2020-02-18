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
import { mockHotspot } from '../../../../helpers/mocks/security-hotspots';
import { mockUser } from '../../../../helpers/testMocks';
import HotspotReviewHistory, { HotspotReviewHistoryProps } from '../HotspotReviewHistory';

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
});

function shallowRender(props?: Partial<HotspotReviewHistoryProps>) {
  const changelogElement: T.IssueChangelog = {
    creationDate: '2018-10-01',
    isUserActive: true,
    user: 'me',
    userName: 'me-name',
    diffs: [
      {
        key: 'assign',
        newValue: 'me',
        oldValue: 'him'
      }
    ]
  };
  const commentElement = {
    key: 'comment-1',
    createdAt: '2018-09-10',
    htmlText: '<strong>TEST</strong>',
    markdown: '*TEST*',
    updatable: true,
    login: 'dude-1',
    user: mockUser({ login: 'dude-1' })
  };
  const commentElement1 = {
    key: 'comment-2',
    createdAt: '2018-09-11',
    htmlText: '<strong>TEST</strong>',
    markdown: '*TEST*',
    updatable: false,
    login: 'dude-2',
    user: mockUser({ login: 'dude-2' })
  };
  const hotspot = mockHotspot({
    creationDate: '2018-09-01',
    changelog: [changelogElement],
    comment: [commentElement, commentElement1]
  });
  return shallow(
    <HotspotReviewHistory
      hotspot={hotspot}
      onDeleteComment={jest.fn()}
      onEditComment={jest.fn()}
      {...props}
    />
  );
}
