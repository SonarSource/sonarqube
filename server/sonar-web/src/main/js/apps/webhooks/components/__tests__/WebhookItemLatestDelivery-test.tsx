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
import { click } from 'sonar-ui-common/helpers/testUtils';
import WebhookItemLatestDelivery from '../WebhookItemLatestDelivery';

const latestDelivery = {
  at: '12.02.2018',
  durationMs: 20,
  httpStatus: 200,
  id: '2',
  success: true
};

const webhook = {
  key: '1',
  name: 'my webhook',
  url: 'http://webhook.target',
  latestDelivery
};

it('should render correctly a success delivery', () => {
  expect(shallow(<WebhookItemLatestDelivery webhook={webhook} />)).toMatchSnapshot();
});

it('should render correctly when no latest delivery', () => {
  expect(
    shallow(<WebhookItemLatestDelivery webhook={{ ...webhook, latestDelivery: undefined }} />)
  ).toMatchSnapshot();
});

it('should render correctly a failed delivery', () => {
  expect(
    shallow(
      <WebhookItemLatestDelivery
        webhook={{
          ...webhook,
          latestDelivery: { ...latestDelivery, httpStatus: 500, success: false }
        }}
      />
    )
  ).toMatchSnapshot();
});

it('should display the latest delivery form', () => {
  const wrapper = shallow(<WebhookItemLatestDelivery webhook={webhook} />);
  click(wrapper.find('ButtonIcon'));
  expect(wrapper.find('LatestDeliveryForm').exists()).toBeTruthy();
});
