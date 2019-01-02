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
import { mount } from 'enzyme';
import { AutoSizerProps } from 'react-virtualized';
import BubbleChart from '../BubbleChart';

jest.mock('react-virtualized/dist/commonjs/AutoSizer', () => ({
  AutoSizer: ({ children }: AutoSizerProps) => children({ width: 100, height: NaN })
}));

it('should display bubbles', () => {
  const items = [{ x: 1, y: 10, size: 7 }, { x: 2, y: 30, size: 5 }];
  const chart = mount(<BubbleChart height={100} items={items} padding={[0, 0, 0, 0]} />);
  chart.find('Bubble').forEach(bubble => expect(bubble).toMatchSnapshot());
});

it('should render bubble links', () => {
  const items = [{ x: 1, y: 10, size: 7, link: 'foo' }, { x: 2, y: 30, size: 5, link: 'bar' }];
  const chart = mount(<BubbleChart height={100} items={items} padding={[0, 0, 0, 0]} />);
  chart.find('Bubble').forEach(bubble => expect(bubble).toMatchSnapshot());
});

it('should render bubbles with click handlers', () => {
  const onClick = jest.fn();
  const items = [{ x: 1, y: 10, size: 7, data: 'foo' }, { x: 2, y: 30, size: 5, data: 'bar' }];
  const chart = mount(
    <BubbleChart height={100} items={items} onBubbleClick={onClick} padding={[0, 0, 0, 0]} />
  );
  chart.find('Bubble').forEach(bubble => expect(bubble).toMatchSnapshot());
});
