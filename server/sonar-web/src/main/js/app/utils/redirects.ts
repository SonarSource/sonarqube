/*
* SonarQube
* Copyright (C) 2009-2017 SonarSource SA
* mailto:contact AT sonarsource DOT com
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
import { RouteProps, RouterState } from 'react-router';
import { LocationDescriptor } from 'history';

export default [
  redirect('/accout/issues', () => ({
    pathname: '/issues',
    query: { myIssues: 'true', resolved: 'false' }
  })),
  plainRedirect('/admin', '/admin/settings'),
  plainRedirect('/background_tasks', '/admin/background_tasks'),
  redirect('/codingrules', () => '/coding_rules' + window.location.hash),
  plainRedirect('/component/index', '/component'),
  plainRedirect('/component_issues', '/project/issues'),
  plainRedirect('/dashboard/index', '/dashboard'),
  redirect('/dashboard/index/:key', nextState => ({
    pathname: '/dashboard',
    query: { id: nextState.params.key }
  })),
  plainRedirect('/governance', '/portfolio'),
  plainRedirect('/groups', '/admin/groups'),
  plainRedirect('/extension/governance/portfolios', '/portfolios'),
  redirect('/issues/search', () => '/issues' + window.location.hash),
  plainRedirect('/metrics', '/admin/custom_metrics'),
  plainRedirect('/permission_templates', '/admin/permission_templates'),
  plainRedirect('/profiles/index', '/profiles'),
  plainRedirect('/projects_admin', '/admin/projects_management'),
  plainRedirect('/quality_gates/index', '/quality_gates'),
  plainRedirect('/roles/global', '/admin/permissions'),
  plainRedirect('/settings', '/admin/settings'),
  plainRedirect('/settings/encryption', '/admin/settings/encryption'),
  plainRedirect('/settings/index', '/admin/settings'),
  plainRedirect('/sessions/login', '/sessions/new'),
  plainRedirect('/system', '/admin/system'),
  plainRedirect('/system/index', '/admin/system'),
  plainRedirect('/view', '/portfolio'),
  plainRedirect('/users', '/admin/users')
];

function redirect(from: string, to: (nextState: RouterState) => LocationDescriptor): RouteProps {
  return {
    path: from,
    onEnter: (nextState, replace) => replace(to(nextState))
  };
}

function plainRedirect(from: string, to: string): RouteProps {
  return redirect(from, ({ location }) => ({ pathname: to, query: location.query }));
}
