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
import { mockSourceLine } from '../../../../helpers/mocks/sources';
import LineCode from '../LineCode';

it('render code', () => {
  expect(shallowRender()).toMatchSnapshot();
  expect(shallowRender({ children: <div>additional child</div> })).toMatchSnapshot(
    'with additional child'
  );
  expect(
    shallowRender({
      secondaryIssueLocations: [
        { index: 1, from: 5, to: 6, line: 16, startLine: 16, text: 'secondary-location-msg' },
      ],
    })
  ).toMatchSnapshot('with secondary location');
});

function shallowRender(props: Partial<LineCode['props']> = {}) {
  return shallow(
    <LineCode
      displayLocationMarkers={true}
      highlightedLocationMessage={{ index: 0, text: 'location description' }}
      highlightedSymbols={['sym-9']}
      issueLocations={[{ from: 0, to: 5, line: 16 }]}
      line={mockSourceLine()}
      onLocationSelect={jest.fn()}
      onSymbolClick={jest.fn()}
      secondaryIssueLocations={[]}
      {...props}
    />
  );
}
