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

import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import {
  DangerButtonPrimary,
  DestructiveIcon,
  GenericAvatar,
  Modal,
  TrashIcon,
  UserGroupIcon,
} from '~design-system';
import { removeGroup } from '../../../api/quality-profiles';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Group } from './ProfilePermissions';

interface Props {
  group: Group;
  onDelete: (group: Group) => void;
  profile: { language: string; name: string };
}

export default function ProfilePermissionsGroup(props: Readonly<Props>) {
  const { group, profile } = props;
  const [deleteDialogOpened, setDeleteDialogOpened] = React.useState(false);

  const handleDelete = () => {
    return removeGroup({
      group: group.name,
      language: profile.language,
      qualityProfile: profile.name,
    }).then(() => {
      setDeleteDialogOpened(false);
      props.onDelete(group);
    });
  };

  return (
    <div className="sw-flex sw-items-center sw-justify-between">
      <div className="sw-flex sw-truncate">
        <GenericAvatar
          Icon={UserGroupIcon}
          className="sw-mt-1/2 sw-mr-3 sw-grow-0 sw-shrink-0"
          name={group.name}
          size="xs"
        />
        <strong className="sw-typo-semibold sw-truncate fs-mask">{group.name}</strong>
      </div>
      <DestructiveIcon
        Icon={TrashIcon}
        aria-label={translateWithParameters(
          'quality_profiles.permissions.remove.group_x',
          group.name,
        )}
        onClick={() => setDeleteDialogOpened(true)}
      />

      {deleteDialogOpened && (
        <Modal
          headerTitle={translate('quality_profiles.permissions.remove.group')}
          onClose={() => setDeleteDialogOpened(false)}
          body={
            <FormattedMessage
              defaultMessage={translate('quality_profiles.permissions.remove.group.confirmation')}
              id="quality_profiles.permissions.remove.group.confirmation"
              values={{
                user: <strong>{group.name}</strong>,
              }}
            />
          }
          primaryButton={
            <DangerButtonPrimary autoFocus onClick={handleDelete}>
              {translate('remove')}
            </DangerButtonPrimary>
          }
          secondaryButtonLabel={translate('cancel')}
        />
      )}
    </div>
  );
}
