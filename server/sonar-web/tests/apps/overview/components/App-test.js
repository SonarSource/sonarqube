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
import { expect } from 'chai';
import { shallow } from 'enzyme';

import App from '../../../../src/main/js/apps/overview/components/App';
import OverviewApp from '../../../../src/main/js/apps/overview/components/OverviewApp';
import EmptyOverview from '../../../../src/main/js/apps/overview/components/EmptyOverview';

describe('Overview :: App', () => {
  it('should render OverviewApp', () => {
    const component = {
      id: 'id',
      snapshotDate: '2016-01-01'
    };

    const output = shallow(
        <App component={component}/>
    );

    expect(output.type())
        .to.equal(OverviewApp);
  });

  it('should render EmptyOverview', () => {
    const component = { id: 'id' };

    const output = shallow(
        <App component={component}/>
    );

    expect(output.type())
        .to.equal(EmptyOverview);
  });

  it('should pass leakPeriodIndex', () => {
    const component = {
      id: 'id',
      snapshotDate: '2016-01-01'
    };

    const output = shallow(
        <App component={component}/>
    );

    expect(output.prop('leakPeriodIndex'))
        .to.equal('1');
  });
});
