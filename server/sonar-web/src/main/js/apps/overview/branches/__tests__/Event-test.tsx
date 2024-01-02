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
import { AnalysisEvent } from '../../../../types/project-activity';
import { Event } from '../Event';

it('should render an event correctly', () => {
  expect(
    shallow(<Event event={{ key: '1', category: 'OTHER', name: 'test' }} />)
  ).toMatchSnapshot();
});

it('should render a version correctly', () => {
  expect(
    shallow(<Event event={{ key: '2', category: 'VERSION', name: '6.5-SNAPSHOT' }} />)
  ).toMatchSnapshot();
});

it('should render rich quality gate event', () => {
  const event: AnalysisEvent = {
    category: 'QUALITY_GATE',
    key: 'foo1234',
    name: '',
    qualityGate: {
      failing: [{ branch: 'master', key: 'foo', name: 'Foo' }],
      status: 'ERROR',
      stillFailing: true,
    },
  };
  expect(shallow(<Event event={event} />)).toMatchSnapshot();
});
