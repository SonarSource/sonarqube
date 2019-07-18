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
import NotificationsSidebar, {
  Feature,
  isUnread,
  Notification,
  Props
} from '../NotificationsSidebar';

const news: Props['news'] = [
  {
    notification: '10 Java rules, Github checks, Security Hotspots, BitBucket branch decoration',
    publicationDate: '2018-04-06',
    features: [
      {
        categories: [{ color: '#ff0000', name: 'Java' }, { color: '#00ff00', name: 'Rules' }],
        description: '10 new Java rules'
      },
      {
        categories: [{ color: '#0000ff', name: 'BitBucket' }],
        description: 'BitBucket branch decoration',
        readMore: 'http://example.com'
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

describe('#NotificationSidebar', () => {
  it('should render correctly if there are new features', () => {
    const wrapper = shallowRender({ loading: true });
    expect(wrapper).toMatchSnapshot();
    wrapper.setProps({ loading: false });
    expect(wrapper).toMatchSnapshot();
    expect(wrapper.find('Notification')).toHaveLength(2);
  });

  it('should render correctly if there are no new unread features', () => {
    const wrapper = shallowRender({
      notificationsLastReadDate: parseDate('2018-12-31')
    });
    expect(wrapper.find('Notification')).toHaveLength(2);
    expect(wrapper.find('Notification[unread=true]')).toHaveLength(0);
  });
});

describe('#isUnread', () => {
  it('should be unread', () => {
    expect(isUnread(0, '2018-12-14', undefined)).toBe(true);
    expect(isUnread(1, '2018-12-14', parseDate('2018-12-12'))).toBe(true);
  });

  it('should be read', () => {
    expect(isUnread(0, '2018-12-16', parseDate('2018-12-16'))).toBe(false);
    expect(isUnread(1, '2018-12-15', undefined)).toBe(false);
  });
});

describe('#Notification', () => {
  it('should render correctly', () => {
    expect(shallow(<Notification notification={news[1]} unread={false} />)).toMatchSnapshot();
    expect(shallow(<Notification notification={news[1]} unread={true} />)).toMatchSnapshot();
  });
});

describe('#Feature', () => {
  it('should render correctly', () => {
    expect(shallow(<Feature feature={news[1].features[0]} />)).toMatchSnapshot();
    expect(shallow(<Feature feature={news[0].features[0]} />)).toMatchSnapshot();
  });
});

function shallowRender(props: Partial<Props> = {}) {
  return shallow(
    <NotificationsSidebar
      fetchMoreFeatureNews={jest.fn()}
      loading={false}
      loadingMore={false}
      news={news}
      notificationsLastReadDate={parseDate('2018-01-01')}
      onClose={jest.fn()}
      paging={{ pageIndex: 1, pageSize: 10, total: 20 }}
      {...props}
    />
  );
}
