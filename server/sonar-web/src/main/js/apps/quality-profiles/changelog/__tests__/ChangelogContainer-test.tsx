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
import { getProfileChangelog } from '../../../../api/quality-profiles';
import { mockLocation, mockQualityProfile, mockRouter } from '../../../../helpers/testMocks';
import { mockEvent, waitAndUpdate } from '../../../../helpers/testUtils';
import { ChangelogContainer } from '../ChangelogContainer';

beforeEach(() => jest.clearAllMocks());

jest.mock('../../../../api/quality-profiles', () => {
  const { mockQualityProfileChangelogEvent } = jest.requireActual('../../../../helpers/testMocks');
  return {
    getProfileChangelog: jest.fn().mockResolvedValue({
      events: [
        mockQualityProfileChangelogEvent(),
        mockQualityProfileChangelogEvent(),
        mockQualityProfileChangelogEvent(),
      ],
      total: 6,
      p: 1,
    }),
  };
});

it('should render correctly without events', async () => {
  (getProfileChangelog as jest.Mock).mockResolvedValueOnce({ events: [] });
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should render correctly', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
});

it('should load more properly', async () => {
  const wrapper = shallowRender();
  await waitAndUpdate(wrapper);

  wrapper.instance().loadMore(mockEvent());
  await waitAndUpdate(wrapper);

  expect(getProfileChangelog).toHaveBeenLastCalledWith(undefined, undefined, expect.anything(), 2);
  expect(wrapper.state().events?.length).toBe(6);
});

function shallowRender() {
  return shallow<ChangelogContainer>(
    <ChangelogContainer
      location={mockLocation()}
      profile={mockQualityProfile()}
      router={mockRouter()}
    />
  );
}
