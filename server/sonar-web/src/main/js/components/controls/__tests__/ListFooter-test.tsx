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
import { click } from '../../../helpers/testUtils';
import { Button } from '../buttons';
import ListFooter, { ListFooterProps } from '../ListFooter';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('default');
  expect(shallowRender({ loading: true })).toMatchSnapshot('loading');
  expect(shallowRender({ needReload: true, reload: jest.fn() })).toMatchSnapshot('reload');
  expect(shallowRender({ loading: true, needReload: true, reload: jest.fn() })).toMatchSnapshot(
    'reload, loading'
  );
  expect(shallowRender({ loadMore: undefined })).toMatchSnapshot(
    'empty if no loadMore nor reload props'
  );
  expect(shallowRender({ count: 5 })).toMatchSnapshot('empty if everything is loaded');
  expect(shallowRender({ total: undefined })).toMatchSnapshot('total undefined');
  expect(shallowRender({ total: undefined, count: 60, pageSize: 30 })).toMatchSnapshot(
    'force show load more button if count % pageSize is 0, and total is undefined'
  );
});

it.each([
  [undefined, 60, 30, true],
  [undefined, 45, 30, false],
  [undefined, 60, undefined, false],
  [60, 60, 30, false],
])(
  'handle showing load more button based on total, count and pageSize',
  (total, count, pageSize, expected) => {
    const wrapper = shallowRender({ total, count, pageSize });
    expect(wrapper.find(Button).exists()).toBe(expected);
  }
);

it('should properly call loadMore', () => {
  const loadMore = jest.fn();
  const wrapper = shallowRender({ loadMore });
  click(wrapper.find(Button));
  expect(loadMore).toHaveBeenCalled();
});

it('should properly call reload', () => {
  const reload = jest.fn();
  const wrapper = shallowRender({ needReload: true, reload });
  click(wrapper.find(Button));
  expect(reload).toHaveBeenCalled();
});

function shallowRender(props: Partial<ListFooterProps> = {}) {
  return shallow<ListFooterProps>(
    <ListFooter count={3} loadMore={jest.fn()} total={5} {...props} />
  );
}
