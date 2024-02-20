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
import styled from '@emotion/styled';
import { Badge, BranchIcon, themeBorder, themeContrast } from 'design-system';
import * as React from 'react';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { collapsePath, limitComponentName } from '../../../helpers/path';
import { ComponentQualifier, isView } from '../../../types/component';
import { Component, Issue } from '../../../types/types';
import { getSelectedLocation } from '../utils';

interface Props {
  component?: Component;
  issue: Issue;
  selectedFlowIndex?: number;
  selectedLocationIndex?: number;
}

export default function ComponentBreadcrumbs({
  component,
  issue,
  selectedFlowIndex,
  selectedLocationIndex,
}: Props) {
  const displayProject =
    !component ||
    ![ComponentQualifier.Project, ComponentQualifier.Directory].includes(
      component.qualifier as ComponentQualifier,
    );

  const displayBranchInformation = isView(component?.qualifier);

  const selectedLocation = getSelectedLocation(issue, selectedFlowIndex, selectedLocationIndex);
  const componentName = selectedLocation ? selectedLocation.componentName : issue.componentLongName;
  const projectName = [issue.projectName, issue.branch].filter((s) => !!s).join(' - ');

  return (
    <DivStyled
      aria-label={translateWithParameters(
        'issues.on_file_x',
        `${displayProject ? issue.projectName + ', ' : ''}${componentName}`,
      )}
      className="sw-flex sw-box-border sw-body-sm sw-w-full sw-pb-2 sw-pt-4 sw-truncate"
    >
      {displayProject && (
        <span title={projectName}>
          {limitComponentName(issue.projectName)}

          {displayBranchInformation && (
            <>
              {' - '}
              {issue.branch ? (
                <>
                  <BranchIcon />
                  <span>{issue.branch}</span>
                </>
              ) : (
                <Badge variant="default">{translate('branches.main_branch')}</Badge>
              )}
            </>
          )}

          <SlashSeparator className="sw-mx-1" />
        </span>
      )}

      <span title={componentName}>{collapsePath(componentName ?? '')}</span>
    </DivStyled>
  );
}

const DivStyled = styled.div`
  color: ${themeContrast('breadcrumb')};
  &:not(:last-child) {
    border-bottom: ${themeBorder('default')};
  }
`;

const SlashSeparator = styled.span`
  &:after {
    content: '/';
    color: rgba(68, 68, 68, 0.3);
  }
`;
