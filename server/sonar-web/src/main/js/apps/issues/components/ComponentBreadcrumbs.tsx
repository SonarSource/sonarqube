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
import * as React from 'react';
import QualifierIcon from 'sonar-ui-common/components/icons/QualifierIcon';
import { translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { collapsePath, limitComponentName } from 'sonar-ui-common/helpers/path';
import { getSelectedLocation } from '../utils';

interface Props {
  component?: T.Component;
  issue: T.Issue;
  selectedFlowIndex?: number;
  selectedLocationIndex?: number;
}

export default function ComponentBreadcrumbs({
  component,
  issue,
  selectedFlowIndex,
  selectedLocationIndex
}: Props) {
  const displayProject = !component || !['TRK', 'BRC', 'DIR'].includes(component.qualifier);
  const displaySubProject = !component || !['BRC', 'DIR'].includes(component.qualifier);

  const selectedLocation = getSelectedLocation(issue, selectedFlowIndex, selectedLocationIndex);
  const componentName = selectedLocation ? selectedLocation.componentName : issue.componentLongName;

  return (
    <div
      aria-label={translateWithParameters(
        'issues.on_file_x',
        `${displayProject ? issue.projectName + ', ' : ''}${componentName}`
      )}
      className="component-name text-ellipsis">
      <QualifierIcon className="spacer-right" qualifier={issue.componentQualifier} />

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

      <span title={componentName}>{collapsePath(componentName || '')}</span>
    </div>
  );
}
