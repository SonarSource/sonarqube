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
import classNames from 'classnames';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import ConfirmButton from '../../../components/controls/ConfirmButton';
import { Button } from '../../../components/controls/buttons';
import WarningIcon from '../../../components/icons/WarningIcon';
import DateFormatter from '../../../components/intl/DateFormatter';
import DateFromNow from '../../../components/intl/DateFromNow';
import Spinner from '../../../components/ui/Spinner';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { useRevokeTokenMutation } from '../../../queries/users';
import { UserToken } from '../../../types/token';

export type TokenDeleteConfirmation = 'inline' | 'modal';

interface Props {
  deleteConfirmation: TokenDeleteConfirmation;
  login: string;
  token: UserToken;
}

export default function TokensFormItem(props: Props) {
  const { token, deleteConfirmation, login } = props;
  const [showConfirmation, setShowConfirmation] = React.useState(false);
  const { mutateAsync, isLoading } = useRevokeTokenMutation();

  const handleRevoke = () => mutateAsync({ login, name: token.name });

  const handleClick = () => {
    if (showConfirmation) {
      handleRevoke()
        .then(() => setShowConfirmation(false))
        .catch(() => setShowConfirmation(false));
    } else {
      setShowConfirmation(true);
    }
  };

  return (
    <tr className={classNames({ 'text-muted-2': token.isExpired })}>
      <td title={token.name} className="hide-overflow nowrap">
        {token.name}
        {token.isExpired && (
          <div className="spacer-top text-warning">
            <WarningIcon className="little-spacer-right" />
            {translate('my_account.tokens.expired')}
          </div>
        )}
      </td>
      <td title={translate('users.tokens', token.type)} className="hide-overflow thin">
        {translate('users.tokens', token.type, 'short')}
      </td>
      <td title={token.project?.name} className="hide-overflow">
        {token.project?.name}
      </td>
      <td className="thin nowrap">
        <DateFromNow date={token.lastConnectionDate} hourPrecision />
      </td>
      <td className="thin nowrap text-right">
        <DateFormatter date={token.createdAt} long />
      </td>
      <td className={classNames('thin nowrap text-right', { 'text-warning': token.isExpired })}>
        {token.expirationDate ? <DateFormatter date={token.expirationDate} long /> : '–'}
      </td>
      <td className="thin nowrap text-right">
        {token.isExpired && (
          <Button
            className="button-red input-small"
            disabled={isLoading}
            onClick={handleRevoke}
            aria-label={translateWithParameters('users.tokens.remove_label', token.name)}
          >
            <Spinner className="little-spacer-right" loading={isLoading}>
              {translate('remove')}
            </Spinner>
          </Button>
        )}
        {!token.isExpired && deleteConfirmation === 'modal' && (
          <ConfirmButton
            confirmButtonText={translate('yes')}
            isDestructive
            modalBody={
              <FormattedMessage
                defaultMessage={translate('users.tokens.sure_X')}
                id="users.tokens.sure_X"
                values={{ token: <strong>{token.name}</strong> }}
              />
            }
            modalHeader={translateWithParameters('users.tokens.revoke_label', token.name)}
            onConfirm={handleRevoke}
          >
            {({ onClick }) => (
              <Button
                className="button-red input-small"
                disabled={isLoading}
                onClick={onClick}
                aria-label={translateWithParameters('users.tokens.revoke_label', token.name)}
              >
                {translate('users.tokens.revoke')}
              </Button>
            )}
          </ConfirmButton>
        )}
        {!token.isExpired && deleteConfirmation === 'inline' && (
          <Button
            className="button-red input-small"
            disabled={isLoading}
            aria-label={
              showConfirmation
                ? translate('users.tokens.sure')
                : translateWithParameters('users.tokens.revoke_label', token.name)
            }
            onClick={handleClick}
          >
            <Spinner className="little-spacer-right" loading={isLoading} />
            {showConfirmation ? translate('users.tokens.sure') : translate('users.tokens.revoke')}
          </Button>
        )}
      </td>
    </tr>
  );
}
