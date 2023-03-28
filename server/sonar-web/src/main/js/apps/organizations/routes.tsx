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
import { Navigate, Route } from "react-router-dom";
import React from "react";
import CreateOrganizationPage from "../create/organization/CreateOrganizationPage";
import OrganizationApp from "./components/OrganizationApp";
import OrganizationProjects from "./components/OrganizationProjects";
import OrganizationEdit from "./components/OrganizationEdit";
import OrganizationDelete from "./components/OrganizationDelete";
import PermissionTemplatesApp from "../permission-templates/components/PermissionTemplatesApp";
import GlobalPermissionsApp from "../permissions/global/components/App";
import GroupsApp from "../groups/components/App";
import ProjectManagementApp from "../projectsManagement/ProjectManagementApp";
import WebhookApp from "../webhooks/components/App";
import IssuesApp from "../issues/components/IssuesApp";
import QualityProfilesApp from "../quality-profiles/components/QualityProfilesApp";
import qualityGatesRoutes from "../quality-gates/routes";
import RulesApp from "../coding-rules/components/App";
import ChangelogContainer from '../quality-profiles/changelog/ChangelogContainer';
import ComparisonContainer from '../quality-profiles/compare/ComparisonContainer';
import ProfileContainer from '../quality-profiles/components/ProfileContainer';
import ProfileDetails from '../quality-profiles/details/ProfileDetails';
import HomeContainer from '../quality-profiles/home/HomeContainer';
import OrganizationPageExtension from "../../app/components/extensions/OrganizationPageExtension";
import OrganizationMembers from "../organizationMembers/OrganizationMembers";

const routes = () => (
    <Route path="organizations">
      <Route path=":organizationKey" element={<OrganizationApp/>}>
        <Route index={true} element={<Navigate to={{ pathname: "projects" }} replace={true}/>}/>
        <Route path="projects" element={<OrganizationProjects />}/>
        <Route path="edit" element={<OrganizationEdit />}/>
        <Route path="delete" element={<OrganizationDelete />}/>
        <Route path="permission_templates" element={<PermissionTemplatesApp />}/>
        <Route path="permissions" element={<GlobalPermissionsApp />}/>
        <Route path="groups" element={<GroupsApp />}/>
        <Route path="projects_management" element={<ProjectManagementApp />}/>
        <Route path="webhooks" element={<WebhookApp />}/>
        <Route path="issues" element={<IssuesApp />}/>
        {qualityGatesRoutes()}
        <Route path="quality_profiles" element={<QualityProfilesApp />}>
           <Route index={true} element={<HomeContainer />} />
           <Route element={<ProfileContainer />}>
             <Route path="show" element={<ProfileDetails />} />
             <Route path="changelog" element={<ChangelogContainer />} />
             <Route path="compare" element={<ComparisonContainer />} />
           </Route>
        </Route>
        <Route path="rules" element={<RulesApp />}/>
        <Route path="members" element={<OrganizationMembers />}/>
        <Route path="extension/:pluginKey/:extensionKey" element={<OrganizationPageExtension />} />
      </Route>
      <Route path="create" element={<CreateOrganizationPage/>}/>
    </Route>
);

export default routes;
