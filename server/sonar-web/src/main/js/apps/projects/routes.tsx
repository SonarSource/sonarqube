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
import React from 'react';
import { Navigate, Route } from 'react-router-dom';
import { save } from '../../helpers/storage';
import CreateProjectPage from '../create/project/CreateProjectPage';
import DefaultPageSelector from './components/DefaultPageSelector';
import FavoriteProjectsContainer from './components/FavoriteProjectsContainer';
import { PROJECTS_ALL, PROJECTS_DEFAULT_FILTER } from './utils';

function PersistNavigate() {
  save(PROJECTS_DEFAULT_FILTER, PROJECTS_ALL);

  return <Navigate to="/projects" replace={true} />;
}

const routes = () => (
  <Route path="projects">
    <Route index={true} element={<DefaultPageSelector />} />
    <Route path="all" element={<PersistNavigate />} />
    <Route path="favorite" element={<FavoriteProjectsContainer />} />
    <Route path="create" element={<CreateProjectPage />} />
  </Route>
);

export default routes;
