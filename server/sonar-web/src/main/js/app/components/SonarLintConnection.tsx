/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { useNavigate, useSearchParams } from 'react-router-dom';
import { generateToken, getTokens } from '../../api/user-tokens';
import { Button } from '../../components/controls/buttons';
import { whenLoggedIn } from '../../components/hoc/whenLoggedIn';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { portIsValid, sendUserToken } from '../../helpers/sonarlint';
import {
  computeTokenExpirationDate,
  getAvailableExpirationOptions,
  getNextTokenName
} from '../../helpers/tokens';
import { LoggedInUser } from '../../types/users';
import './SonarLintConnection.css';

interface Props {
  currentUser: LoggedInUser;
}

const getNextAvailableTokenName = async (login: string, tokenNameBase: string) => {
  const tokens = await getTokens(login);

  return getNextTokenName(tokenNameBase, tokens);
};

async function computeExpirationDate() {
  const options = await getAvailableExpirationOptions();
  const maxOption = options[options.length - 1];

  return maxOption.value ? computeTokenExpirationDate(maxOption.value) : undefined;
}

export function SonarLintConnection({ currentUser }: Props) {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [success, setSuccess] = React.useState(false);
  const [error, setError] = React.useState('');
  const [newTokenName, setNewTokenName] = React.useState('');

  const port = parseInt(searchParams.get('port') ?? '0', 10);
  const ideName = searchParams.get('ideName') ?? translate('sonarlint-connection.unspecified-ide');

  // If the port is not in the expected range, redirect to home page
  React.useEffect(() => {
    if (!portIsValid(port)) {
      navigate('/');
    }
  }, [navigate, port]);

  const { login } = currentUser;

  const authorize = React.useCallback(async () => {
    const newTokenName = await getNextAvailableTokenName(login, `sonarlint-${ideName}`);
    const expirationDate = await computeExpirationDate();
    const token = await generateToken({ name: newTokenName, login, expirationDate });

    try {
      await sendUserToken(port, token);
      setSuccess(true);
      setNewTokenName(newTokenName);
    } catch (error) {
      if (error instanceof Error) {
        setError(error.message);
      } else {
        setError('-');
      }
    }
  }, [port, ideName, login]);

  return (
    <div className="sonarlint-connection-page">
      <div className="sonarlint-connection-content boxed-group">
        <div className="boxed-group-inner text-center">
          <h1 className="big-spacer-bottom">{translate('sonarlint-connection.title')}</h1>
          {error && (
            <>
              <p className="big big-spacer-bottom">{translate('sonarlint-connection.error')}</p>
              <p className="monospaced huge-spacer-bottom">{error}</p>
            </>
          )}
          {success && (
            <p className="big">
              {translateWithParameters('sonarlint-connection.success', newTokenName)}
            </p>
          )}

          {!error && !success && (
            <>
              <p className="big big-spacer-bottom">
                {translateWithParameters('sonarlint-connection.description', ideName)}
              </p>
              <p className="big huge-spacer-bottom">
                {translate('sonarlint-connection.description2')}
              </p>

              <Button className="big-spacer-bottom" onClick={authorize}>
                {translate('sonarlint-connection.action')}
              </Button>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

export default whenLoggedIn(SonarLintConnection);
