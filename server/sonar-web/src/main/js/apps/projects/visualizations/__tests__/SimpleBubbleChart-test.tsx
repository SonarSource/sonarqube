/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { ComponentQualifier } from '../../../../types/component';
import { Project } from '../../types';
import SimpleBubbleChart from '../SimpleBubbleChart';

it('renders', () => {
  const project1: Project = {
    key: 'foo',
    measures: { complexity: '17.2', coverage: '53.5', ncloc: '1734', security_rating: '2' },
    name: 'Foo',
    qualifier: ComponentQualifier.Project,
    tags: [],
    visibility: 'public'
  };
  const app = {
    ...project1,
    key: 'app',
    measures: { complexity: '23.1', coverage: '87.3', ncloc: '32478', security_rating: '1' },
    name: 'App',
    qualifier: ComponentQualifier.Application
  };
  expect(
    shallow(
      <SimpleBubbleChart
        colorMetric="security_rating"
        displayOrganizations={false}
        helpText="foobar"
        projects={[app, project1]}
        sizeMetric={{ key: 'ncloc', type: 'INT' }}
        xMetric={{ key: 'complexity', type: 'INT' }}
        yMetric={{ key: 'coverage', type: 'PERCENT' }}
      />
    )
  ).toMatchSnapshot();
});
