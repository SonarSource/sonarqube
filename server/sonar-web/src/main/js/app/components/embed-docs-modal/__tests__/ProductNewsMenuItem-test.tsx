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
import { ProductNewsMenuItem } from '../ProductNewsMenuItem';
import { fetchPrismicRefs, fetchPrismicNews } from '../../../../api/news';
import { waitAndUpdate } from '../../../../helpers/testUtils';

jest.mock('../../../../api/news', () => ({
  fetchPrismicRefs: jest.fn().mockResolvedValue({ id: 'master', ref: 'master-ref' }),
  fetchPrismicNews: jest.fn().mockResolvedValue([
    {
      data: { title: 'My Product News' },
      last_publication_date: '2018-04-06T12:07:19+0000',
      uid: 'my-product-news'
    }
  ])
}));

it('should load the product news', async () => {
  const wrapper = shallow(<ProductNewsMenuItem accessToken="token" tag="SonarCloud" />);
  expect(wrapper).toMatchSnapshot();
  await waitAndUpdate(wrapper);
  expect(fetchPrismicRefs).toHaveBeenCalled();
  expect(fetchPrismicNews).toHaveBeenCalledWith({
    accessToken: 'token',
    ref: 'master-ref',
    tag: 'SonarCloud'
  });
  expect(wrapper).toMatchSnapshot();
});
