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
import React from 'react';
import { MemoryRouter, Params, Route, Routes } from 'react-router-dom';
import { CatchAll } from '../../../helpers/testReactTestingUtils';
import { Dict } from '../../../types/types';
import NavigateWithParams from '../NavigateWithParams';

it('should transform path parameters to search params', () => {
  const transformParams = jest.fn((params: Params) => {
    return { also: 'this', ...params };
  });

  renderNavigateWithParams(transformParams);

  expect(transformParams).toHaveBeenCalled();
  expect(screen.getByText('/target?also=this&key=hello&subkey=test')).toBeInTheDocument();
});

function renderNavigateWithParams(transformParams: (params: Params) => Dict<string>) {
  render(
    <MemoryRouter initialEntries={['/source/hello/test']}>
      <Routes>
        <Route
          path="/source/:key/:subkey"
          element={<NavigateWithParams pathname="/target" transformParams={transformParams} />}
        />
        <Route path="*" element={<CatchAll />} />
      </Routes>
    </MemoryRouter>,
  );
}
