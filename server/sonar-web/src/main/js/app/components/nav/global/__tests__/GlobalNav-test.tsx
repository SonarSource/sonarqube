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
import * as React from 'react';
import { shallow } from 'enzyme';
import { GlobalNav } from '../GlobalNav';
import { isSonarCloud } from '../../../../../helpers/system';
import { waitAndUpdate, click } from '../../../../../helpers/testUtils';
import {
  fetchPrismicRefs,
  fetchPrismicFeatureNews,
  PrismicFeatureNews
} from '../../../../../api/news';

jest.mock('../../../../../helpers/system', () => ({ isSonarCloud: jest.fn() }));

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
    fetchPrismicFeatureNews: jest.fn().mockResolvedValue({
      news: prismicResult,
      paging: { pageIndex: 1, pageSize: 10, total: 2 }
    })
  };
});

const appState: GlobalNav['props']['appState'] = {
  globalPages: [],
  canAdmin: false,
  organizationsEnabled: false,
  qualifiers: []
};
const location = { pathname: '' };

beforeEach(() => {
  (fetchPrismicRefs as jest.Mock).mockClear();
  (fetchPrismicFeatureNews as jest.Mock).mockClear();
});

it('should render for SonarQube', async () => {
  (isSonarCloud as jest.Mock).mockImplementation(() => false);

  const wrapper = shallowRender();

  expect(wrapper).toMatchSnapshot();
  wrapper.setProps({ currentUser: { isLoggedIn: true } });
  expect(wrapper.find('[data-test="global-nav-plus"]').exists()).toBe(true);

  await waitAndUpdate(wrapper);
  expect(fetchPrismicRefs).not.toBeCalled();
});

it('should render for SonarCloud', () => {
  (isSonarCloud as jest.Mock).mockImplementation(() => true);
  const wrapper = shallowRender({ currentUser: { isLoggedIn: true } });

  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('[data-test="global-nav-plus"]').exists()).toBe(true);
});

it('should render correctly if there are new features', async () => {
  (isSonarCloud as jest.Mock).mockImplementation(() => true);
  const wrapper = shallowRender();
  wrapper.setProps({ currentUser: { isLoggedIn: true } });

  await waitAndUpdate(wrapper);
  expect(fetchPrismicRefs).toHaveBeenCalled();
  expect(fetchPrismicFeatureNews).toHaveBeenCalled();
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('NavLatestNotification').exists()).toBe(true);
  click(wrapper.find('NavLatestNotification'));
  expect(wrapper.find('NotificationsSidebar').exists()).toBe(true);
});

function shallowRender(props: Partial<GlobalNav['props']> = {}) {
  return shallow(
    <GlobalNav
      accessToken="token"
      appState={appState}
      currentUser={{ isLoggedIn: false }}
      location={location}
      setCurrentUserSetting={jest.fn()}
      {...props}
    />
  );
}
