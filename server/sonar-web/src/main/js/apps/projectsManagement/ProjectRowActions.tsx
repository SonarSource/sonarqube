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
import React, { useState } from 'react';
import { getComponentNavigation } from '../../api/navigation';
import { Project } from '../../api/project-management';
import ActionsDropdown, { ActionsDropdownItem } from '../../components/controls/ActionsDropdown';
import Spinner from '../../components/ui/Spinner';
import { throwGlobalError } from '../../helpers/error';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { getComponentPermissionsUrl } from '../../helpers/urls';
import { useGithubProvisioningEnabledQuery } from '../../queries/identity-provider/github';
import { LoggedInUser } from '../../types/users';
import ApplyTemplate from '../permissions/project/components/ApplyTemplate';
import RestoreAccessModal from './RestoreAccessModal';

export interface Props {
  currentUser: Pick<LoggedInUser, 'login' | 'local'>;
  project: Project;
}

export default function ProjectRowActions({ currentUser, project }: Props) {
  const [applyTemplateModal, setApplyTemplateModal] = useState(false);
  const [hasAccess, setHasAccess] = useState<boolean | undefined>(undefined);
  const [loading, setLoading] = useState(false);
  const [restoreAccessModal, setRestoreAccessModal] = useState(false);
  const { data: githubProvisioningEnabled } = useGithubProvisioningEnabledQuery();

  const fetchPermissions = async () => {
    setLoading(true);

    try {
      const { configuration } = await getComponentNavigation({ component: project.key });
      const hasAccess = Boolean(configuration?.showPermissions && configuration?.canBrowseProject);
      setHasAccess(hasAccess);
      setLoading(false);
    } catch (error) {
      throwGlobalError(error);
    } finally {
      setLoading(false);
    }
  };

  const handleDropdownOpen = () => {
    if (hasAccess === undefined && !loading) {
      fetchPermissions();
    }
  };

  const handleRestoreAccessDone = () => {
    setRestoreAccessModal(false);
    setHasAccess(true);
  };

  return (
    <>
      <ActionsDropdown
        label={translateWithParameters('projects_management.show_actions_for_x', project.name)}
        onOpen={handleDropdownOpen}
      >
        {loading ? (
          <ActionsDropdownItem>
            <Spinner />
          </ActionsDropdownItem>
        ) : (
          <>
            {hasAccess === true && (
              <ActionsDropdownItem
                className="js-edit-permissions"
                to={getComponentPermissionsUrl(project.key)}
              >
                {translate(project.managed ? 'show_permissions' : 'edit_permissions')}
              </ActionsDropdownItem>
            )}

            {hasAccess === false &&
              (!project.managed || currentUser.local || !githubProvisioningEnabled) && (
                <ActionsDropdownItem
                  className="js-restore-access"
                  onClick={() => setRestoreAccessModal(true)}
                >
                  {translate('global_permissions.restore_access')}
                </ActionsDropdownItem>
              )}
          </>
        )}

        {!project.managed && (
          <ActionsDropdownItem
            className="js-apply-template"
            onClick={() => setApplyTemplateModal(true)}
          >
            {translate('projects_role.apply_template')}
          </ActionsDropdownItem>
        )}
      </ActionsDropdown>

      {restoreAccessModal && (
        <RestoreAccessModal
          currentUser={currentUser}
          onClose={() => setRestoreAccessModal(false)}
          onRestoreAccess={handleRestoreAccessDone}
          project={project}
        />
      )}

      {applyTemplateModal && (
        <ApplyTemplate onClose={() => setApplyTemplateModal(false)} project={project} />
      )}
    </>
  );
}
