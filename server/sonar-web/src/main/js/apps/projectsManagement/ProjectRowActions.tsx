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
import { ActionsDropdown, ItemButton, ItemLink, PopupZLevel, Spinner } from 'design-system';
import { noop } from 'lodash';
import React, { useState } from 'react';
import { throwGlobalError } from '~sonar-aligned/helpers/error';
import { getComponentNavigation } from '../../api/navigation';
import { Project } from '../../api/project-management';
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
        id="project-management-action-dropdown"
        toggleClassName="it__user-actions-toggle"
        onOpen={handleDropdownOpen}
        allowResizing
        ariaLabel={translateWithParameters('projects_management.show_actions_for_x', project.name)}
        zLevel={PopupZLevel.Global}
      >
        <Spinner loading={loading} className="sw-flex sw-ml-3">
          <>
            {hasAccess === true && (
              <ItemLink to={getComponentPermissionsUrl(project.key)}>
                {translate(project.managed ? 'show_permissions' : 'edit_permissions')}
              </ItemLink>
            )}

            {hasAccess === false &&
            (!project.managed || currentUser.local || !githubProvisioningEnabled) ? (
              <ItemButton
                className="it__restore-access"
                onClick={() => setRestoreAccessModal(true)}
              >
                {translate('global_permissions.restore_access')}
              </ItemButton>
            ) : (
              hasAccess === false && (
                <ItemButton disabled onClick={noop}>
                  {translate('global_permissions.no_actions_available')}
                </ItemButton>
              )
            )}
          </>
        </Spinner>

        {!project.managed && (
          <ItemButton className="it__apply-template" onClick={() => setApplyTemplateModal(true)}>
            {translate('projects_role.apply_template')}
          </ItemButton>
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
