/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { getSelectedLocation } from '../utils';
import Organization from '../../../components/shared/Organization';
import { collapsePath, limitComponentName } from '../../../helpers/path';

interface Props {
  component?: T.Component;
  issue: Pick<
    T.Issue,
    | 'component'
    | 'componentLongName'
    | 'flows'
    | 'organization'
    | 'project'
    | 'projectName'
    | 'secondaryLocations'
    | 'subProject'
    | 'subProjectName'
  >;
  link?: boolean;
  organization: { key: string } | undefined;
  selectedFlowIndex?: number;
  selectedLocationIndex?: number;
}

export default function ComponentBreadcrumbs({
  component,
  issue,
  organization,
  selectedFlowIndex,
  selectedLocationIndex
}: Props) {
  const displayOrganization =
    !organization && (!component || ['VW', 'SVW'].includes(component.qualifier));
  const displayProject = !component || !['TRK', 'BRC', 'DIR'].includes(component.qualifier);
  const displaySubProject = !component || !['BRC', 'DIR'].includes(component.qualifier);

  const selectedLocation = getSelectedLocation(issue, selectedFlowIndex, selectedLocationIndex);
  const componentName = selectedLocation ? selectedLocation.componentName : issue.componentLongName;

  return (
    <div className="component-name text-ellipsis">
      {displayOrganization && <Organization link={false} organizationKey={issue.organization} />}

      {displayProject && (
        <span title={issue.projectName}>
          {limitComponentName(issue.projectName)}
          <span className="slash-separator" />
        </span>
      )}

      {displaySubProject && issue.subProject !== undefined && issue.subProjectName !== undefined && (
        <span title={issue.subProjectName}>
          {limitComponentName(issue.subProjectName)}
          <span className="slash-separator" />
        </span>
      )}

      {collapsePath(componentName || '')}
    </div>
  );
}
