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
  Checkbox,
  DangerButtonPrimary,
  FlagMessage,
  LightPrimary,
  Link,
  Modal,
} from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { useDocUrl } from '../../../helpers/docs';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { useDeactivateUserMutation } from '../../../queries/users';
import { RestUserDetailed } from '../../../types/users';

export interface Props {
  onClose: () => void;
  user: RestUserDetailed;
}

const DEACTIVATE_FORM_ID = 'deactivate-user-form';

export default function DeactivateForm(props: Props) {
  const { user } = props;
  const [anonymize, setAnonymize] = React.useState(false);

  const { mutate: deactivateUser, isLoading } = useDeactivateUserMutation();

  const handleDeactivate = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    deactivateUser(
      { id: user.id, anonymize },
      {
        onSuccess: props.onClose,
      },
    );
  };

  const header = translate('users.deactivate_user');
  const docUrl = useDocUrl('/instance-administration/authentication/overview/');

  return (
    <Modal
      headerTitle={header}
      body={
        <form autoComplete="off" id={DEACTIVATE_FORM_ID} onSubmit={handleDeactivate}>
          {translateWithParameters('users.deactivate_user.confirmation', user.name, user.login)}
          <Checkbox
            id="delete-user"
            className="sw-flex sw-items-center sw-mt-4"
            checked={anonymize}
            onCheck={(checked) => setAnonymize(checked)}
          >
            <LightPrimary className="sw-ml-3">{translate('users.delete_user')}</LightPrimary>
          </Checkbox>
          {anonymize && (
            <FlagMessage variant="warning" className="sw-mt-2">
              <span>
                <FormattedMessage
                  defaultMessage={translate('users.delete_user.help')}
                  id="delete-user-warning"
                  values={{
                    link: <Link to={docUrl}>{translate('users.delete_user.help.link')}</Link>,
                  }}
                />
              </span>
            </FlagMessage>
          )}
        </form>
      }
      onClose={props.onClose}
      loading={isLoading}
      primaryButton={
        <DangerButtonPrimary form={DEACTIVATE_FORM_ID} disabled={isLoading} type="submit">
          {translate('users.deactivate')}
        </DangerButtonPrimary>
      }
      secondaryButtonLabel={translate('cancel')}
    />
  );
}
