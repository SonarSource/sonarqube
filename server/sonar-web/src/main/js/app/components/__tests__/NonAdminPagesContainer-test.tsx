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
import { render, screen } from '@testing-library/react';
import * as React from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { mockComponent } from '../../../helpers/mocks/component';
import { ComponentContextShape } from '../../../types/component';
import { Component } from '../../../types/types';
import NonAdminPagesContainer from '../NonAdminPagesContainer';
import { ComponentContext } from '../componentContext/ComponentContext';

function Child() {
  return <div>Test Child</div>;
}

it('should render correctly for an user that does not have access to all children', () => {
  renderNonAdminPagesContainer(
    mockComponent({ qualifier: ComponentQualifier.Application, canBrowseAllChildProjects: false }),
  );
  expect(screen.getByText(/^application.cannot_access_all_child_projects1/)).toBeInTheDocument();
});

it('should render correctly', () => {
  renderNonAdminPagesContainer(mockComponent());
  expect(screen.getByText('Test Child')).toBeInTheDocument();
});

function renderNonAdminPagesContainer(component: Component) {
  return render(
    <ComponentContext.Provider value={{ component } as ComponentContextShape}>
      <MemoryRouter>
        <Routes>
          <Route element={<NonAdminPagesContainer />}>
            <Route path="*" element={<Child />} />
          </Route>
        </Routes>
      </MemoryRouter>
    </ComponentContext.Provider>,
  );
}
