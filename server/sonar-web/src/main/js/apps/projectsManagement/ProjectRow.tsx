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
import { Project } from '../../api/project-management';
import Link from '../../components/common/Link';
import PrivacyBadgeContainer from '../../components/common/PrivacyBadgeContainer';
import Checkbox from '../../components/controls/Checkbox';
import Tooltip from '../../components/controls/Tooltip';
import QualifierIcon from '../../components/icons/QualifierIcon';
import DateFormatter from '../../components/intl/DateFormatter';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { getComponentOverviewUrl } from '../../helpers/urls';
import { useGithubProvisioningEnabledQuery } from '../../queries/identity-provider/github';
import { ComponentQualifier } from '../../types/component';
import { LoggedInUser } from '../../types/users';
import './ProjectRow.css';
import ProjectRowActions from './ProjectRowActions';

interface Props {
  currentUser: Pick<LoggedInUser, 'login'>;
  onProjectCheck: (project: Project, checked: boolean) => void;
  project: Project;
  selected: boolean;
}

export default function ProjectRow(props: Props) {
  const { currentUser, project, selected } = props;
  const { data: githubProvisioningEnabled } = useGithubProvisioningEnabledQuery();

  const handleProjectCheck = (checked: boolean) => {
    props.onProjectCheck(project, checked);
  };

  return (
    <tr data-project-key={project.key}>
      <td className="thin">
        <Checkbox
          label={translateWithParameters('projects_management.select_project', project.name)}
          checked={selected}
          onCheck={handleProjectCheck}
        />
      </td>

      <td className="nowrap hide-overflow project-row-text-cell">
        <Link
          className="link-no-underline"
          to={getComponentOverviewUrl(project.key, project.qualifier)}
        >
          <QualifierIcon className="little-spacer-right" qualifier={project.qualifier} />

          <Tooltip overlay={project.name} placement="left">
            <span>{project.name}</span>
          </Tooltip>
        </Link>
        {project.qualifier === ComponentQualifier.Project &&
          githubProvisioningEnabled &&
          !project.managed && <span className="badge sw-ml-1">{translate('local')}</span>}
      </td>

      <td className="thin nowrap">
        <PrivacyBadgeContainer qualifier={project.qualifier} visibility={project.visibility} />
      </td>

      <td className="nowrap hide-overflow project-row-text-cell">
        <Tooltip overlay={project.key} placement="left">
          <span className="note">{project.key}</span>
        </Tooltip>
      </td>

      <td className="thin nowrap text-right">
        {project.lastAnalysisDate ? (
          <DateFormatter date={project.lastAnalysisDate} />
        ) : (
          <span className="note">—</span>
        )}
      </td>

      <td className="thin nowrap">
        <ProjectRowActions currentUser={currentUser} project={project} />
      </td>
    </tr>
  );
}
