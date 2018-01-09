/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as React from 'react';
import { Link } from 'react-router';
import Organization from '../../../components/shared/Organization';
import { collapsePath, limitComponentName } from '../../../helpers/path';
import { getProjectUrl } from '../../../helpers/urls';
import { Component } from '../../../app/types';

interface Props {
  branch?: string;
  component?: Component;
  issue: {
    component: string;
    componentLongName: string;
    organization: string;
    project: string;
    projectName: string;
    subProject?: string;
    subProjectName?: string;
  };
  organization?: { key: string };
}

export default function ComponentBreadcrumbs({ branch, component, issue, organization }: Props) {
  const displayOrganization =
    !organization && (component == null || ['VW', 'SVW'].includes(component.qualifier));
  const displayProject = component == null || !['TRK', 'BRC', 'DIR'].includes(component.qualifier);
  const displaySubProject = component == null || !['BRC', 'DIR'].includes(component.qualifier);

  return (
    <div className="component-name text-ellipsis">
      {displayOrganization && (
        <Organization linkClassName="link-no-underline" organizationKey={issue.organization} />
      )}

      {displayProject && (
        <span title={issue.projectName}>
          <Link to={getProjectUrl(issue.project, branch)} className="link-no-underline">
            {limitComponentName(issue.projectName)}
          </Link>
          <span className="slash-separator" />
        </span>
      )}

      {displaySubProject &&
        issue.subProject !== undefined &&
        issue.subProjectName !== undefined && (
          <span title={issue.subProjectName}>
            <Link to={getProjectUrl(issue.subProject, branch)} className="link-no-underline">
              {limitComponentName(issue.subProjectName)}
            </Link>
            <span className="slash-separator" />
          </span>
        )}

      <Link to={getProjectUrl(issue.component, branch)} className="link-no-underline">
        <span title={issue.componentLongName}>{collapsePath(issue.componentLongName)}</span>
      </Link>
    </div>
  );
}
