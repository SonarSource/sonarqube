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
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockIssue } from '../../../../helpers/testMocks';
import { ComponentQualifier } from '../../../../types/component';
import ComponentBreadcrumbs from '../ComponentBreadcrumbs';

const baseIssue = mockIssue(false, {
  component: 'comp',
  componentLongName: 'comp-name',
  componentQualifier: ComponentQualifier.File,
  project: 'proj',
  projectName: 'proj-name',
  branch: 'test-branch',
});

it('renders', () => {
  expect(
    shallow(<ComponentBreadcrumbs component={undefined} issue={baseIssue} />)
  ).toMatchSnapshot();
});

it('renders issues properly for views', () => {
  expect(
    shallow(
      <ComponentBreadcrumbs
        component={mockComponent({ qualifier: ComponentQualifier.Portfolio })}
        issue={{ ...baseIssue, branch: undefined }}
      />
    )
  ).toMatchSnapshot();
  expect(
    shallow(
      <ComponentBreadcrumbs
        component={mockComponent({ qualifier: ComponentQualifier.Portfolio })}
        issue={{ ...baseIssue }}
      />
    )
  ).toMatchSnapshot('with branch information');
});
