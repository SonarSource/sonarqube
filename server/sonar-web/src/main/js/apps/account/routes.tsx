/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { Route } from 'react-router-dom';
import Account from './Account';
import Notifications from './notifications/Notifications';
import Profile from './profile/Profile';
import ProjectsContainer from './projects/ProjectsContainer';
import Security from './security/Security';
import UserOrganizations from './organizations/UserOrganizations';

const routes = () => (
  <Route path="account" element={<Account />}>
    <Route index={true} element={<Profile />} />
    <Route path="security" element={<Security />} />
    <Route path="projects" element={<ProjectsContainer />} />
    <Route path="notifications" element={<Notifications />} />
    <Route path="organizations" element={<UserOrganizations />} />
  </Route>
);

export default routes;
