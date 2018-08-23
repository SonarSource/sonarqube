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
import BranchMeasures, { BranchCoverage } from '../BranchMeasures';

const MEASURES = [
  { metric: 'new_coverage', value: '0', periods: [{ index: 1, value: '95.9943' }] },
  { metric: 'coverage', value: '99.3' }
];

describe('BranchMeasures', () => {
  it('should render coverage measures', () => {
    expect(shallow(<BranchMeasures measures={MEASURES} />)).toMatchSnapshot();
  });

  it('should render correctly when a coverage measure is missing', () => {
    expect(shallow(<BranchMeasures measures={[MEASURES[0]]} />)).toMatchSnapshot();
  });

  it('should not render anything', () => {
    expect(shallow(<BranchMeasures measures={[]} />).type()).toBeNull();
  });
});

describe('BranchCoverage', () => {
  it('should render correctly', () => {
    expect(shallow(<BranchCoverage measure={MEASURES[1]} />)).toMatchSnapshot();
  });

  it('should render leak measure correctly', () => {
    expect(shallow(<BranchCoverage measure={MEASURES[0]} />)).toMatchSnapshot();
  });
});
