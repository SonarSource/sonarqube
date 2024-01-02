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
import { screen } from '@testing-library/react';
import * as React from 'react';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockIssue } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { ComponentQualifier } from '../../../../types/component';
import { Component, Issue } from '../../../../types/types';
import ComponentBreadcrumbs from '../ComponentBreadcrumbs';

const baseIssue = mockIssue(false, {
  component: 'comp',
  componentLongName: 'comp-name',
  componentQualifier: ComponentQualifier.File,
  project: 'proj',
  projectName: 'proj-name',
  branch: 'test-branch',
});

describe('renders properly', () => {
  it('without component with issue', () => {
    renderComponentBreadcrumbs(mockComponent());

    expect(screen.getByLabelText('issues.on_file_x.comp-name')).toBeInTheDocument();
  });

  it('with component without issue branch', () => {
    renderComponentBreadcrumbs(mockComponent({ qualifier: ComponentQualifier.Portfolio }), {
      branch: undefined,
    });

    expect(screen.getByLabelText('issues.on_file_x.proj-name, comp-name')).toBeInTheDocument();
    expect(screen.queryByText('test-branch')).not.toBeInTheDocument();
  });

  it('with component and issue branch', () => {
    renderComponentBreadcrumbs(mockComponent({ qualifier: ComponentQualifier.Portfolio }));

    expect(screen.getByLabelText('issues.on_file_x.proj-name, comp-name')).toBeInTheDocument();
    expect(screen.getByText('test-branch')).toBeInTheDocument();
  });
});

function renderComponentBreadcrumbs(component?: Component, issue: Partial<Issue> = {}) {
  return renderComponent(
    <ComponentBreadcrumbs component={component} issue={{ ...baseIssue, ...issue }} />,
  );
}
