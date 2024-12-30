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

import { Route } from 'react-router-dom';
import { lazyLoadComponent } from '~sonar-aligned/helpers/lazyLoadComponent';

const QualityProfilesApp = lazyLoadComponent(() => import('./components/QualityProfilesApp'));
const HomeContainer = lazyLoadComponent(() => import('./home/HomeContainer'));
const ProfileContainer = lazyLoadComponent(() => import('./components/ProfileContainer'));
const ProfileDetails = lazyLoadComponent(() => import('./details/ProfileDetails'));
const ChangelogContainer = lazyLoadComponent(() => import('./changelog/ChangelogContainer'));
const ComparisonContainer = lazyLoadComponent(() => import('./compare/ComparisonContainer'));

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
