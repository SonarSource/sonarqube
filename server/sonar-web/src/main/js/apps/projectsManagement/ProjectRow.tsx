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

import { Checkbox, LinkHighlight, LinkStandalone } from '@sonarsource/echoes-react';
import { ActionCell, Badge, ContentCell, Note, TableRow } from 'design-system';
import * as React from 'react';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { Project } from '../../api/project-management';
import PrivacyBadgeContainer from '../../components/common/PrivacyBadgeContainer';
import Tooltip from '../../components/controls/Tooltip';
import DateFormatter from '../../components/intl/DateFormatter';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { getComponentOverviewUrl } from '../../helpers/urls';
import { useGithubProvisioningEnabledQuery } from '../../queries/identity-provider/github';
import { useGilabProvisioningEnabledQuery } from '../../queries/identity-provider/gitlab';
import { LoggedInUser } from '../../types/users';
import ProjectRowActions from './ProjectRowActions';

interface Props {
  currentUser: Pick<LoggedInUser, 'login'>;
  onProjectCheck: (project: Project, checked: boolean) => void;
  project: Project;
  selected: boolean;
}

export default function ProjectRow(props: Readonly<Props>) {
  const { currentUser, project, selected } = props;
  const { data: githubProvisioningEnabled } = useGithubProvisioningEnabledQuery();
  const { data: gitlabProbivisioningEnabled } = useGilabProvisioningEnabledQuery();

  const handleProjectCheck = (checked: boolean) => {
    props.onProjectCheck(project, checked);
  };

  return (
    <TableRow data-project-key={project.key}>
      <ContentCell>
        <Checkbox
          ariaLabel={translateWithParameters('projects_management.select_project', project.name)}
          checked={selected}
          onCheck={handleProjectCheck}
        />
      </ContentCell>
      <ContentCell className="it__project-row-text-cell">
        <LinkStandalone
          highlight={LinkHighlight.CurrentColor}
          to={getComponentOverviewUrl(project.key, project.qualifier)}
        >
          <Tooltip content={project.name} side="left">
            <span>{project.name}</span>
          </Tooltip>
        </LinkStandalone>
        {project.qualifier === ComponentQualifier.Project &&
          (githubProvisioningEnabled || gitlabProbivisioningEnabled) &&
          !project.managed && <Badge className="sw-ml-1">{translate('local')}</Badge>}
      </ContentCell>
      <ContentCell>
        <PrivacyBadgeContainer qualifier={project.qualifier} visibility={project.visibility} />
      </ContentCell>
      <ContentCell className="it__project-row-text-cell">
        <Tooltip content={project.key} side="left">
          <Note>{project.key}</Note>
        </Tooltip>
      </ContentCell>
      <ContentCell>
        {project.lastAnalysisDate ? (
          <DateFormatter date={project.lastAnalysisDate} />
        ) : (
          <Note>—</Note>
        )}
      </ContentCell>
      <ActionCell>
        <ProjectRowActions currentUser={currentUser} project={project} />
      </ActionCell>
    </TableRow>
  );
}
