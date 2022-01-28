/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import React from 'react';
import { LineCoverage, LineCoverageProps } from '../LineCoverage';

it('should render correctly', () => {
  expect(shallowRender()).toMatchSnapshot('covered');
  expect(shallowRender({ line: { line: 3, coverageStatus: 'uncovered' } })).toMatchSnapshot(
    'uncovered'
  );
  expect(shallowRender({ line: { line: 3, coverageStatus: 'partially-covered' } })).toMatchSnapshot(
    'partially covered, 0 conditions'
  );
  expect(
    shallowRender({ line: { line: 3, coverageStatus: 'partially-covered', coveredConditions: 10 } })
  ).toMatchSnapshot('partially covered, 10 conditions');
  expect(shallowRender({ line: { line: 3, coverageStatus: undefined } })).toMatchSnapshot(
    'no data'
  );
});

it('should correctly trigger a scroll', () => {
  const element = { current: {} };
  jest.spyOn(React, 'useEffect').mockImplementation(f => f());
  jest.spyOn(React, 'useRef').mockImplementation(() => element);

  const scroll = jest.fn();
  shallowRender({ scroll, scrollToUncoveredLine: true });
  expect(scroll).toHaveBeenCalledWith(element.current);

  scroll.mockReset();
  shallowRender({ scroll, scrollToUncoveredLine: false });
  expect(scroll).not.toHaveBeenCalled();
});

function shallowRender(props: Partial<LineCoverageProps> = {}) {
  return shallow(<LineCoverage line={{ line: 3, coverageStatus: 'covered' }} {...props} />);
}
