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
import { Route } from 'react-router-dom';
import ChangelogContainer from './changelog/ChangelogContainer';
import ComparisonContainer from './compare/ComparisonContainer';
import ProfileContainer from './components/ProfileContainer';
import QualityProfilesApp from './components/QualityProfilesApp';
import ProfileDetails from './details/ProfileDetails';
import HomeContainer from './home/HomeContainer';

export enum QualityProfilePath {
  SHOW = 'show',
  CHANGELOG = 'changelog',
  COMPARE = 'compare',
}
const routes = () => (
  <Route path="profiles" element={<QualityProfilesApp />}>
    <Route index element={<HomeContainer />} />
    <Route element={<ProfileContainer />}>
      <Route path={QualityProfilePath.SHOW} element={<ProfileDetails />} />
      <Route path={QualityProfilePath.CHANGELOG} element={<ChangelogContainer />} />
      <Route path={QualityProfilePath.COMPARE} element={<ComparisonContainer />} />
    </Route>
  </Route>
);

export default routes;
