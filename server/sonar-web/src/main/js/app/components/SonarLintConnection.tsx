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
import { FormattedMessage } from 'react-intl';
import { useSearchParams } from 'react-router-dom';
import { generateToken, getTokens } from '../../api/user-tokens';
import Link from '../../components/common/Link';
import { Button } from '../../components/controls/buttons';
import { ClipboardButton } from '../../components/controls/clipboard';
import { whenLoggedIn } from '../../components/hoc/whenLoggedIn';
import CheckIcon from '../../components/icons/CheckIcon';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { portIsValid, sendUserToken } from '../../helpers/sonarlint';
import { getScannableProjects } from '../../api/components';
import Select, { BasicSelectOption } from '../../components/controls/Select';
import {
  computeTokenExpirationDate,
  computeTokenExpirationDateByHours,
  getAvailableExpirationOptions,
  getNextTokenName,
} from '../../helpers/tokens';
import { NewUserToken, TokenExpiration } from '../../types/token';
import { LoggedInUser } from '../../types/users';
import './SonarLintConnection.css';
import withAppStateContext from './app-state/withAppStateContext';
import { AppState } from '../../types/appstate';
import { isDeploymentForAmazon} from '../../helpers/urls';
import { TokenType } from '../../types/token';

enum Status {
  request,
  tokenError,
  tokenCreated,
  tokenSent,
}

interface Props {
  currentUser: LoggedInUser;
  appState: AppState
}

const TOKEN_PREFIX = 'CodeScan';

const getNextAvailableTokenName = async (login: string, tokenNameBase: string) => {
  const tokens = await getTokens(login);

  return getNextTokenName(tokenNameBase, tokens);
};

async function computeExpirationDate(whiteLabel: string) {
  return isDeploymentForAmazon(whiteLabel) ? computeTokenExpirationDateByHours(8) : computeTokenExpirationDate(1);
}

export function SonarLintConnection({ appState, currentUser }: Props) {
  const { whiteLabel } = appState;
  const [searchParams] = useSearchParams();
  const [status, setStatus] = React.useState(Status.request);
  const [newToken, setNewToken] = React.useState<NewUserToken | undefined>(undefined);

  const port = parseInt(searchParams.get('port') ?? '0', 10);
  const ideName = searchParams.get('ideName') ?? translate('sonarlint-connection.unspecified-ide');

  const { login } = currentUser;

  const [projects, setProjects] = React.useState<BasicSelectOption[]>([]);
  const [selectedProject, setSelectedProject] = React.useState<BasicSelectOption>(undefined);

  React.useEffect(() => {
    const fetchProjects = async () => {
        const { projects: projectArray } = await getScannableProjects();
        const projects = projectArray.map((project) => ({ label: project.name, value: project.key }));

        setProjects(projects);
        setSelectedProject(projects.length === 1 ? projects[0] : undefined);
    };
    fetchProjects();
  },[]);

  const handleProjectChange = (selectedProject: BasicSelectOption) => {
    setSelectedProject(selectedProject);
  };

  const isAllowConnectionDisabled = () => {
    return isDeploymentForAmazon(whiteLabel) && (selectedProject == undefined || selectedProject == null);
  }

  const authorize = React.useCallback(async () => {
    const newTokenName = await getNextAvailableTokenName(login, `${TOKEN_PREFIX}-${ideName}`);
    const expirationDate = await computeExpirationDate(whiteLabel);
    const token = isDeploymentForAmazon(whiteLabel) ? await generateToken({ name: newTokenName, login, expirationDate, projectKey: selectedProject.value, type: TokenType.Project }).catch(
      () => undefined) : await generateToken({ name: newTokenName, login, expirationDate }).catch(
      () => undefined
     );

    if (!token) {
      setStatus(Status.tokenError);
      return;
    }

    setNewToken(token);

    if (!portIsValid(port)) {
      setStatus(Status.tokenCreated);
      return;
    }

    try {
      await sendUserToken(port, token);
      setStatus(Status.tokenSent);
    } catch (_) {
      setStatus(Status.tokenCreated);
    }
  }, [port, ideName, login, selectedProject]);

  return (
    <div className="sonarlint-connection-page">
      <div className="sonarlint-connection-content boxed-group">
        <div className="boxed-group-inner text-center">
          {status === Status.request && (
            <>
              <h1 className="big-spacer-top big-spacer-bottom">
                {translate('sonarlint-connection.request.title')}
              </h1>
              <p className="big big-spacer-top big-spacer-bottom">
                {translateWithParameters('sonarlint-connection.request.description', ideName)}
              </p>
              <p className="big huge-spacer-bottom">
                {translate('sonarlint-connection.request.description2')}
              </p>

              <div className="display-flex-center big-spacer-bottom display-flex-justify-center">
                  {isDeploymentForAmazon(whiteLabel) && (
                      <div className="input-large spacer-right display-flex-column">
                          <label htmlFor="token-select-project" className="text-bold text-left">
                            {translate('users.tokens.project')}
                          </label>
                          <Select
                            id="token-select-project"
                            className="spacer-top it__project"
                            onChange={handleProjectChange}
                            options={projects}
                            placeholder={translate('users.tokens.select_project')}
                            value={selectedProject}
                          />
                      </div>
                  )}

                  <Button className="it__generate-token" onClick={authorize} style={{ marginTop: 'auto' }} disabled={isAllowConnectionDisabled()}>
                    <CheckIcon className="spacer-right" />
                    {translate('sonarlint-connection.request.action')}
                  </Button>
              </div>
            </>
          )}

          {status === Status.tokenError && (
            <>
              <img
                alt=""
                aria-hidden={true}
                className="big-spacer-top big-spacer-bottom padded-top"
                src="/images/cross.svg"
              />
              <h1 className="big-spacer-bottom">
                {translate('sonarlint-connection.token-error.title')}
              </h1>
              <p className="big big-spacer-top big-spacer-bottom">
                {translate('sonarlint-connection.token-error.description')}
              </p>
              <p className="big huge-spacer-bottom">
                <FormattedMessage
                  id="sonarlint-connection.token-error.description2"
                  defaultMessage={translate('sonarlint-connection.token-error.description2')}
                  values={{
                    link: (
                      <Link to="/account/security">
                        {translate('sonarlint-connection.token-error.description2.link')}
                      </Link>
                    ),
                  }}
                />
              </p>
            </>
          )}

          {status === Status.tokenCreated && newToken && (
            <>
              <img
                alt=""
                aria-hidden={true}
                className="big-spacer-top big-spacer-bottom padded-top"
                src="/images/check.svg"
              />
              <h1 className="big-spacer-bottom">
                {translate('sonarlint-connection.connection-error.title')}
              </h1>
              <p className="big big-spacer-top big-spacer-bottom">
                {translate('sonarlint-connection.connection-error.description')}
              </p>
              <div className="display-flex-center">
                <span className="field-label">
                  {translate('sonarlint-connection.connection-error.token-name')}
                </span>
                {newToken.name}
              </div>
              <hr />
              <div className="display-flex-center">
                <span className="field-label">
                  {translate('sonarlint-connection.connection-error.token-value')}
                </span>
                <span className="sonarlint-token-value">{newToken.token}</span>
                <ClipboardButton className="big-spacer-left" copyValue={newToken.token} />
              </div>
              <div className="big huge-spacer-top">
                <strong>{translate('sonarlint-connection.connection-error.next-steps')}</strong>
              </div>
              <ol className="big big-spacer-top big-spacer-bottom">
                <li>{translate('sonarlint-connection.connection-error.step1')}</li>
                <li>{translate('sonarlint-connection.connection-error.step2')}</li>
              </ol>
            </>
          )}

          {status === Status.tokenSent && newToken && (
            <>
              <h1 className="big-spacer-top big-spacer-bottom">
                {translate('sonarlint-connection.success.title')}
              </h1>
              <p className="big big-spacer-top big-spacer-bottom">
                {translateWithParameters('sonarlint-connection.success.description', newToken.name)}
              </p>
              <div className="big huge-spacer-top">
                <strong>{translate('sonarlint-connection.success.last-step')}</strong>
              </div>
              <div className="big big-spacer-top big-spacer-bottom">
                {translate('sonarlint-connection.success.step')}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

export default whenLoggedIn(withAppStateContext(SonarLintConnection));
