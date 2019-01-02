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
import BranchMeasures, { BranchCoverage, BranchDuplications } from '../BranchMeasures';

const MEASURES = [
  { metric: 'new_coverage', value: '0', periods: [{ index: 1, value: '95.9943' }] },
  { metric: 'new_duplicated_lines_density', periods: [{ index: 1, value: '3.5' }] }
];

const pr: T.PullRequest = { base: 'master', branch: 'feature-x', key: '5', title: '' };

describe('BranchMeasures', () => {
  it('should render coverage and duplications', () => {
    expect(
      shallow(<BranchMeasures branchLike={pr} componentKey="foo" measures={MEASURES} />)
    ).toMatchSnapshot();
  });

  it('should render correctly when coverage is missing', () => {
    expect(
      shallow(<BranchMeasures branchLike={pr} componentKey="foo" measures={[MEASURES[1]]} />)
    ).toMatchSnapshot();
  });

  it('should not render anything', () => {
    expect(
      shallow(<BranchMeasures branchLike={pr} componentKey="foo" measures={[]} />).type()
    ).toBeNull();
  });
});

describe('BranchCoverage', () => {
  it('should render correctly', () => {
    expect(
      shallow(<BranchCoverage branchLike={pr} componentKey="foo" measure={MEASURES[0]} />)
    ).toMatchSnapshot();
  });
});

describe('BranchDuplications', () => {
  it('should render correctly', () => {
    expect(
      shallow(<BranchDuplications branchLike={pr} componentKey="foo" measure={MEASURES[1]} />)
    ).toMatchSnapshot();
  });
});
