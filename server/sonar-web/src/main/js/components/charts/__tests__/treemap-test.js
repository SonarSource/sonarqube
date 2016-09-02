/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import React from 'react';
import { shallow } from 'enzyme';
import { Treemap, TreemapRect } from '../treemap';

describe('Treemap', function () {

  it('should display', function () {
    const items = [
      { size: 10, color: '#777', label: 'SonarQube :: Server' },
      { size: 30, color: '#777', label: 'SonarQube :: Web' },
      { size: 20, color: '#777', label: 'SonarQube :: Search' }
    ];
    const chart = shallow(
        <Treemap
            items={items}
            width={100}
            height={100}
            breadcrumbs={[]}
            canBeClicked={() => true}/>);
    expect(chart.find(TreemapRect).length).toBe(3);
  });

});
