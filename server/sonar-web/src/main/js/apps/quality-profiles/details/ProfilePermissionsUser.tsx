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
import {
  Avatar,
  DangerButtonPrimary,
  DestructiveIcon,
  Modal,
  Note,
  TrashIcon,
} from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { removeUser } from '../../../api/quality-profiles';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { UserSelected } from '../../../types/types';

interface Props {
  onDelete: (user: UserSelected) => void;
  profile: { language: string; name: string };
  user: UserSelected;
  organization: string;
}

export default function ProfilePermissionsGroup(props: Readonly<Props>) {
  const { user, profile } = props;
  const [deleteDialogOpened, setDeleteDialogOpened] = React.useState(false);

  const handleDelete = () => {
    return removeUser({
      language: profile.language,
      login: user.login,
      qualityProfile: profile.name,
      organization: props.organization,
    }).then(() => {
      setDeleteDialogOpened(false);
      props.onDelete(user);
    });
  };

  return (
    <div className="sw-flex sw-items-center sw-justify-between">
      <div className="sw-flex sw-truncate">
        <Avatar
          className="sw-mt-1/2 sw-mr-3 sw-grow-0 sw-shrink-0"
          hash={user.avatar}
          name={user.name}
          size="xs"
        />
        <div className="sw-truncate fs-mask">
          <strong className="sw-typo-semibold">{user.name}</strong>
          <Note className="sw-block">{user.login}</Note>
        </div>
      </div>
      <DestructiveIcon
        Icon={TrashIcon}
        aria-label={translateWithParameters(
          'quality_profiles.permissions.remove.user_x',
          user.name,
        )}
        onClick={() => setDeleteDialogOpened(true)}
      />

      {deleteDialogOpened && (
        <Modal
          headerTitle={translate('quality_profiles.permissions.remove.user')}
          onClose={() => setDeleteDialogOpened(false)}
          body={
            <FormattedMessage
              defaultMessage={translate('quality_profiles.permissions.remove.user.confirmation')}
              id="quality_profiles.permissions.remove.user.confirmation"
              values={{
                user: <strong>{user.name}</strong>,
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
