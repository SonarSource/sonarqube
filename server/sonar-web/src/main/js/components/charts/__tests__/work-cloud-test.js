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
import TestUtils from 'react-addons-test-utils';
import { expect } from 'chai';

import { WordCloud } from '../word-cloud';

describe('Word Cloud', function () {

  it('should display', function () {
    const items = [
      { size: 10, link: '#', text: 'SonarQube :: Server' },
      { size: 30, link: '#', text: 'SonarQube :: Web' },
      { size: 20, link: '#', text: 'SonarQube :: Search' }
    ];
    const chart = TestUtils.renderIntoDocument(<WordCloud items={items} width={100} height={100}/>);
    expect(TestUtils.scryRenderedDOMComponentsWithTag(chart, 'a')).to.have.length(3);
  });

});
