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
import { shallow } from 'enzyme';
import * as React from 'react';
import { parseDate } from 'sonar-ui-common/helpers/dates';
import { PrismicFeatureNews } from '../../../../api/news';
import NavLatestNotification from '../NavLatestNotification';

it('should render correctly if there are new features, and the user has not opted out', () => {
  const wrapper = shallowRender();
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('.navbar-latest-notification')).toHaveLength(1);
});

it('should render correctly if there are new features, but the user has opted out', () => {
  const wrapper = shallowRender({ notificationsOptOut: true });
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('.navbar-latest-notification')).toHaveLength(0);
});

it('should render correctly if there are no new unread features', () => {
  const wrapper = shallowRender({
    notificationsLastReadDate: parseDate('2018-12-31T12:07:19+0000')
  });
  expect(wrapper).toMatchSnapshot();
  expect(wrapper.find('.navbar-latest-notification')).toHaveLength(0);
});

function shallowRender(props: Partial<NavLatestNotification['props']> = {}) {
  const lastNews: PrismicFeatureNews = {
    notification: '10 Java rules, Github checks, Security Hotspots, BitBucket branch decoration',
    publicationDate: '2018-04-06',
    features: [
      {
        categories: [{ color: '#ff0000', name: 'Java' }],
        description: '10 new Java rules'
      }
    ]
  };
  return shallow(
    <NavLatestNotification
      lastNews={lastNews}
      notificationsLastReadDate={parseDate('2018-01-01T12:07:19+0000')}
      notificationsOptOut={false}
      onClick={jest.fn()}
      setCurrentUserSetting={jest.fn()}
      {...props}
    />
  );
}
