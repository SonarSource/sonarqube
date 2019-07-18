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
import { shallow } from 'enzyme';
import * as React from 'react';
import { mockComponentMeasure, mockMainBranch } from '../../../../helpers/testMocks';
import ComponentName, { getTooltip, mostCommonPrefix, Props } from '../ComponentName';

describe('#getTooltip', () => {
  it('should correctly format component information', () => {
    expect(getTooltip(mockComponentMeasure(true))).toMatchSnapshot();
    expect(getTooltip(mockComponentMeasure(true, { qualifier: 'UTS' }))).toMatchSnapshot();
    expect(getTooltip(mockComponentMeasure(true, { path: undefined }))).toMatchSnapshot();
    expect(getTooltip(mockComponentMeasure(false))).toMatchSnapshot();
  });
});

describe('#mostCommonPrefix', () => {
  it('should correctly find the common path prefix', () => {
    expect(mostCommonPrefix(['src/main/ts/tests', 'src/main/java/tests'])).toEqual('src/main/');
    expect(mostCommonPrefix(['src/main/ts/app', 'src/main/ts/app'])).toEqual('src/main/ts/');
    expect(mostCommonPrefix(['src/main/ts', 'lib/main/ts'])).toEqual('');
  });
});

describe('#ComponentName', () => {
  it('should render correctly for files', () => {
    expect(shallowRender()).toMatchSnapshot();
    expect(shallowRender({ canBrowse: true })).toMatchSnapshot();
    expect(
      shallowRender({ rootComponent: mockComponentMeasure(false, { qualifier: 'TRK' }) })
    ).toMatchSnapshot();
    expect(
      shallowRender({ rootComponent: mockComponentMeasure(false, { qualifier: 'APP' }) })
    ).toMatchSnapshot();
    expect(
      shallowRender({
        component: mockComponentMeasure(true, { branch: 'foo' }),
        rootComponent: mockComponentMeasure(false, { qualifier: 'APP' })
      })
    ).toMatchSnapshot();
  });

  it('should render correctly for dirs', () => {
    expect(
      shallowRender({
        component: mockComponentMeasure(false, { name: 'src/main/ts/app', qualifier: 'DIR' }),
        previous: mockComponentMeasure(false, { name: 'src/main/ts/tests', qualifier: 'DIR' })
      })
    ).toMatchSnapshot();
    expect(
      shallowRender({
        component: mockComponentMeasure(false, { name: 'src', qualifier: 'DIR' }),
        previous: mockComponentMeasure(false, { name: 'lib', qualifier: 'DIR' })
      })
    ).toMatchSnapshot();
  });

  it('should render correctly for refs', () => {
    expect(
      shallowRender({
        component: mockComponentMeasure(false, {
          branch: 'foo',
          refKey: 'src/main/ts/app',
          qualifier: 'TRK'
        })
      })
    ).toMatchSnapshot();
    expect(
      shallowRender({
        component: mockComponentMeasure(false, {
          branch: 'foo',
          refKey: 'src/main/ts/app',
          qualifier: 'TRK'
        }),
        rootComponent: mockComponentMeasure(false, { qualifier: 'APP' })
      })
    ).toMatchSnapshot();
  });
});

function shallowRender(props: Partial<Props> = {}) {
  return shallow(
    <ComponentName
      branchLike={mockMainBranch()}
      component={mockComponentMeasure(true)}
      rootComponent={mockComponentMeasure()}
      {...props}
    />
  );
}
