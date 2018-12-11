/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as React from 'react';
import { shallow } from 'enzyme';
import { GlobalNavNotifications } from '../GlobalNavNotifications';
import { waitAndUpdate } from '../../../../../helpers/testUtils';
import {
  fetchPrismicRefs,
  fetchPrismicFeatureNews,
  PrismicFeatureNews
} from '../../../../../api/news';
import { parseDate } from '../../../../../helpers/dates';

// Solve redux warning issue "No reducer provided for key":
// https://stackoverflow.com/questions/43375079/redux-warning-only-appearing-in-tests
jest.mock('../../../../../store/rootReducer');

jest.mock('../../../../../api/news', () => {
  const prismicResult: PrismicFeatureNews[] = [
    {
      notification: '10 Java rules, Github checks, Security Hotspots, BitBucket branch decoration',
      publicationDate: '2018-04-06',
      features: [
        {
          categories: [{ color: '#ff0000', name: 'Java' }],
          description: '10 new Java rules'
        }
      ]
    },
    {
      notification: 'Some other notification',
      publicationDate: '2018-04-05',
      features: [
        {
          categories: [{ color: '#0000ff', name: 'BitBucket' }],
          description: 'BitBucket branch decoration',
          readMore: 'http://example.com'
        }
      ]
    }
  ];

  return {
    fetchPrismicRefs: jest.fn().mockResolvedValue({ ref: 'master-ref' }),
    fetchPrismicFeatureNews: jest.fn().mockResolvedValue(prismicResult)
  };
});

beforeEach(() => {
  (fetchPrismicRefs as jest.Mock).mockClear();
  (fetchPrismicFeatureNews as jest.Mock).mockClear();
});

it('should render correctly if there are new features, and the user has not opted out', async () => {
  const wrapper = shallowRender();
  expect(wrapper.type()).toBeNull();

  await waitAndUpdate(wrapper);
  expect(fetchPrismicRefs).toHaveBeenCalled();
  expect(fetchPrismicFeatureNews).toHaveBeenCalled();
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('.navbar-latest-notification')).toHaveLength(1);
});

it('should render correctly if there are new features, but the user has opted out', async () => {
  const wrapper = shallowRender({ notificationsOptOut: true });

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('.navbar-latest-notification')).toHaveLength(0);
});

it('should render correctly if there are no new unread features', async () => {
  const wrapper = shallowRender({
    notificationsLastReadDate: parseDate('2018-12-31T12:07:19+0000')
  });

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('.navbar-latest-notification')).toHaveLength(0);
});

it('should render correctly if there are no new features', async () => {
  (fetchPrismicFeatureNews as jest.Mock<any>).mockResolvedValue([]);

  const wrapper = shallowRender();

  await waitAndUpdate(wrapper);
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('.navbar-latest-notification')).toHaveLength(0);
});

function shallowRender(props: Partial<GlobalNavNotifications['props']> = {}) {
  return shallow(
    <GlobalNavNotifications
      accessToken="token"
      fetchCurrentUserSettings={jest.fn()}
      notificationsLastReadDate={parseDate('2018-01-01T12:07:19+0000')}
      notificationsOptOut={false}
      setCurrentUserSetting={jest.fn()}
      {...props}
    />
  );
}
