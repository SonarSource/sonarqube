/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import { mount } from 'enzyme';
import * as React from 'react';
import TreeMap from '../TreeMap';
import TreeMapRect from '../TreeMapRect';

it('should render correctly', () => {
  const items = [
    { key: '1', size: 10, color: '#777', label: 'SonarQube :: Server' },
    { key: '2', size: 30, color: '#777', label: 'SonarQube :: Web' },
    {
      key: '3',
      size: 20,
      gradient: '#777',
      label: 'SonarQube :: Search',
      metric: { key: 'coverage', type: 'PERCENT' },
    },
  ];
  const onRectClick = jest.fn();
  const chart = mount(
    <TreeMap height={100} items={items} onRectangleClick={onRectClick} width={100} />
  );
  const rects = chart.find(TreeMapRect);
  expect(rects).toHaveLength(3);

  const event: React.MouseEvent<HTMLAnchorElement> = {
    stopPropagation: jest.fn(),
  } as any;

  (rects.first().instance() as TreeMapRect).handleLinkClick(event);
  expect(event.stopPropagation).toHaveBeenCalled();

  (rects.first().instance() as TreeMapRect).handleRectClick();
  expect(onRectClick).toHaveBeenCalledWith('2');
});
