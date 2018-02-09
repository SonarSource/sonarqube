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
import WebhookItem, { LatestDelivery } from '../WebhookItem';

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

it('should render correctly', () => {
  expect(
    shallow(
      <WebhookItem
        onDelete={jest.fn(() => Promise.resolve())}
        onUpdate={jest.fn(() => Promise.resolve())}
        webhook={webhook}
      />
    )
  ).toMatchSnapshot();
});

it('should render correctly the latest delivery', () => {
  expect(shallow(<LatestDelivery latestDelivery={undefined} />)).toMatchSnapshot();
  expect(shallow(<LatestDelivery latestDelivery={latestDelivery} />)).toMatchSnapshot();
  expect(
    shallow(
      <LatestDelivery latestDelivery={{ ...latestDelivery, httpStatus: 500, success: false }} />
    )
  ).toMatchSnapshot();
});
