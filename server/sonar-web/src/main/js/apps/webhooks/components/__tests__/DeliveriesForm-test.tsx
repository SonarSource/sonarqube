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
import DeliveriesForm from '../DeliveriesForm';
import { searchDeliveries } from '../../../../api/webhooks';

jest.mock('../../../../api/webhooks', () => ({
  searchDeliveries: jest.fn(() =>
    Promise.resolve({
      deliveries: [
        {
          at: '12.02.2018',
          durationMs: 20,
          httpStatus: 200,
          id: '2',
          success: true
        },
        {
          at: '11.02.2018',
          durationMs: 122,
          httpStatus: 500,
          id: '1',
          success: false
        }
      ],
      paging: {
        pageIndex: 1,
        pageSize: 10,
        total: 15
      }
    })
  )
}));

const webhook = { key: '1', name: 'foo', url: 'http://foo.bar' };

beforeEach(() => {
  (searchDeliveries as jest.Mock<any>).mockClear();
});

it('should render correctly', async () => {
  const wrapper = getWrapper();
  expect(wrapper).toMatchSnapshot();

  await new Promise(setImmediate);
  expect(searchDeliveries as jest.Mock<any>).lastCalledWith({ webhook: webhook.key, ps: 10 });
  wrapper.update();
  expect(wrapper).toMatchSnapshot();

  wrapper.find('ListFooter').prop<Function>('loadMore')();
  expect(searchDeliveries).lastCalledWith({ webhook: webhook.key, p: 2, ps: 10 });
});

function getWrapper(props = {}) {
  return shallow(<DeliveriesForm onClose={jest.fn()} webhook={webhook} {...props} />);
}
