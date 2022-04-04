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
import * as React from 'react';
import { LineSCM, LineSCMProps } from '../LineSCM';

it('should render correctly', () => {
  const scmInfo = { scmRevision: 'foo', scmAuthor: 'foo', scmDate: '2017-01-01' };

  expect(shallowRender()).toMatchSnapshot('default');
  expect(
    shallowRender({ line: { line: 3, ...scmInfo }, previousLine: { line: 2, ...scmInfo } })
  ).toMatchSnapshot('same commit');
  expect(shallowRender({ line: { line: 3, scmDate: '2017-01-01' } })).toMatchSnapshot('no author');
});

function shallowRender(props: Partial<LineSCMProps> = {}) {
  return shallow(
    <LineSCM
      line={{ line: 3, scmAuthor: 'foo', scmDate: '2017-01-01' }}
      previousLine={{ line: 2, scmAuthor: 'bar', scmDate: '2017-01-02' }}
      {...props}
    />
  );
}
