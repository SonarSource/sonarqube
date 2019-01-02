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
import Event from '../Event';
import { RichQualityGateEvent } from '../../../projectActivity/components/RichQualityGateEventInner';

const EVENT = { key: '1', category: 'OTHER', name: 'test' };
const VERSION = { key: '2', category: 'VERSION', name: '6.5-SNAPSHOT' };

it('should render an event correctly', () => {
  expect(shallow(<Event event={EVENT} />)).toMatchSnapshot();
});

it('should render a version correctly', () => {
  expect(shallow(<Event event={VERSION} />)).toMatchSnapshot();
});

it('should render rich quality gate event', () => {
  const event: RichQualityGateEvent = {
    category: 'QUALITY_GATE',
    key: 'foo1234',
    name: '',
    qualityGate: {
      failing: [{ branch: 'master', key: 'foo', name: 'Foo' }],
      status: 'ERROR',
      stillFailing: true
    }
  };
  expect(shallow(<Event event={event} />)).toMatchSnapshot();
});
