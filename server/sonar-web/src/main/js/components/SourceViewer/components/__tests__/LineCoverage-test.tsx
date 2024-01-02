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
import { LineCoverage, LineCoverageProps } from '../LineCoverage';

jest.mock('react', () => {
  return {
    ...jest.requireActual('react'),
    useRef: jest.fn(),
    useEffect: jest.fn(),
  };
});

it('should correctly trigger a scroll', () => {
  const scroll = jest.fn();
  const element = { current: { scrollIntoView: scroll } };
  (React.useEffect as jest.Mock).mockImplementation((f) => f());
  (React.useRef as jest.Mock).mockImplementation(() => element);

  shallowRender({ scrollToUncoveredLine: true });
  expect(scroll).toHaveBeenCalled();

  scroll.mockReset();
  shallowRender({ scrollToUncoveredLine: false });
  expect(scroll).not.toHaveBeenCalled();
});

function shallowRender(props: Partial<LineCoverageProps> = {}) {
  return shallow(<LineCoverage line={{ line: 3, coverageStatus: 'covered' }} {...props} />);
}
