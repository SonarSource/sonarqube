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
import { waitAndUpdate } from 'sonar-ui-common/helpers/testUtils';
import { commentSecurityHotspot } from '../../../../api/security-hotspots';
import { mockHotspot } from '../../../../helpers/mocks/security-hotspots';
import HotspotViewerReviewHistoryTabCommentBox, {
  HotspotViewerReviewHistoryTabCommentBoxProps
} from '../HotspotViewerReviewHistoryTabCommentBox';

jest.mock('../../../../api/security-hotspots', () => ({
  commentSecurityHotspot: jest.fn().mockResolvedValue(null)
}));

it('should render correctly', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();

  // Show the comment box
  wrapper.find('#hotspot-comment-box-display').simulate('click');
  expect(wrapper).toMatchSnapshot('with comment box');

  // Cancel comment
  wrapper.find('#hotspot-comment-box-cancel').simulate('click');
  expect(wrapper).toMatchSnapshot('without comment box');
});

it('should properly submit a comment', async () => {
  const hotspot = mockHotspot();
  const onUpdateHotspot = jest.fn();
  const wrapper = shallowRender({ hotspot, onUpdateHotspot });

  wrapper.find('#hotspot-comment-box-display').simulate('click');
  wrapper.find('textarea').simulate('change', { target: { value: 'tata' } });
  wrapper.find('#hotspot-comment-box-submit').simulate('click');

  await waitAndUpdate(wrapper);

  expect(commentSecurityHotspot).toHaveBeenCalledWith(hotspot.key, 'tata');
  expect(onUpdateHotspot).toHaveBeenCalled();

  expect(wrapper).toMatchSnapshot('without comment box');
});

function shallowRender(props?: Partial<HotspotViewerReviewHistoryTabCommentBoxProps>) {
  return shallow<HotspotViewerReviewHistoryTabCommentBoxProps>(
    <HotspotViewerReviewHistoryTabCommentBox
      hotspot={mockHotspot()}
      onUpdateHotspot={jest.fn()}
      {...props}
    />
  );
}
