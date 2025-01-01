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

import { Button, ButtonVariety, Text, TextSize } from '@sonarsource/echoes-react';
import { FishVisual } from '~design-system';
import { useRouter } from '~sonar-aligned/components/hoc/withRouter';
import { useCurrentUser } from '../../../app/components/current-user/CurrentUserContext';
import { translate } from '../../../helpers/l10n';
import { hasGlobalPermission } from '../../../helpers/users';
import { Permissions } from '../../../types/permissions';
import { isLoggedIn } from '../../../types/users';

export default function EmptyInstance() {
  const { currentUser } = useCurrentUser();
  const router = useRouter();
  const showNewProjectButton =
    isLoggedIn(currentUser) && hasGlobalPermission(currentUser, Permissions.ProjectCreation);

  return (
    <div className="sw-flex sw-flex-col sw-items-center sw-py-8">
      <FishVisual />
      <Text isHighlighted size={TextSize.Large} className="sw-mt-6">
        {showNewProjectButton
          ? translate('projects.no_projects.empty_instance.new_project')
          : translate('projects.no_projects.empty_instance')}
      </Text>
      {showNewProjectButton && (
        <>
          <p className="sw-mt-2 sw-typo-default">
            {translate('projects.no_projects.empty_instance.how_to_add_projects')}
          </p>
          <Button
            className="sw-mt-6"
            onClick={() => {
              router.push('/projects/create');
            }}
            variety={ButtonVariety.Primary}
          >
            {translate('my_account.create_new.TRK')}
          </Button>
        </>
      )}
    </div>
  );
}
