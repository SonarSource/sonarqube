/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import { Route, IndexRedirect } from 'react-router';
import OrganizationPage from './components/OrganizationPage';
import OrganizationProjects from './components/OrganizationProjects';
import OrganizationFavoriteProjects from './components/OrganizationFavoriteProjects';
import OrganizationAdmin from './components/OrganizationAdmin';
import OrganizationEdit from './components/OrganizationEdit';
import OrganizationGroups from './components/OrganizationGroups';
import OrganizationPermissions from './components/OrganizationPermissions';
import OrganizationPermissionTemplates from './components/OrganizationPermissionTemplates';
import OrganizationProjectsManagement from './components/OrganizationProjectsManagement';
import OrganizationDelete from './components/OrganizationDelete';

export default (
    <Route path=":organizationKey" component={OrganizationPage}>
      <IndexRedirect to="projects"/>
      <Route path="projects" component={OrganizationProjects}/>
      <Route path="projects/favorite" component={OrganizationFavoriteProjects}/>
      <Route component={OrganizationAdmin}>
        <Route path="delete" component={OrganizationDelete}/>
        <Route path="edit" component={OrganizationEdit}/>
        <Route path="groups" component={OrganizationGroups}/>
        <Route path="permissions" component={OrganizationPermissions}/>
        <Route path="permission_templates" component={OrganizationPermissionTemplates}/>
        <Route path="projects_management" component={OrganizationProjectsManagement}/>
      </Route>
    </Route>
);
