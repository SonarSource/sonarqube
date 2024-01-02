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
import { isEmpty } from 'lodash';
import * as React from 'react';
import { getScannableProjects } from '../../../api/components';
import withCurrentUserContext from '../../../app/components/current-user/withCurrentUserContext';
import Select, { LabelValueSelectOption } from '../../../components/controls/Select';
import { SubmitButton } from '../../../components/controls/buttons';
import Spinner from '../../../components/ui/Spinner';
import { translate } from '../../../helpers/l10n';
import {
  EXPIRATION_OPTIONS,
  computeTokenExpirationDate,
  getAvailableExpirationOptions,
} from '../../../helpers/tokens';
import { hasGlobalPermission } from '../../../helpers/users';
import { useGenerateTokenMutation, useUserTokensQuery } from '../../../queries/users';
import { Permissions } from '../../../types/permissions';
import { TokenExpiration, TokenType } from '../../../types/token';
import { CurrentUser } from '../../../types/users';
import TokensFormItem, { TokenDeleteConfirmation } from './TokensFormItem';
import TokensFormNewToken from './TokensFormNewToken';

interface Props {
  deleteConfirmation: TokenDeleteConfirmation;
  login: string;
  displayTokenTypeInput: boolean;
  currentUser: CurrentUser;
}

export function TokensForm(props: Props) {
  const { currentUser, deleteConfirmation, displayTokenTypeInput, login } = props;
  const { data: tokens, isLoading: loading } = useUserTokensQuery(login);
  const [newToken, setNewToken] = React.useState<{ name: string; token: string }>();
  const [newTokenName, setNewTokenName] = React.useState('');
  const [newTokenType, setNewTokenType] = React.useState<TokenType>();
  const [projects, setProjects] = React.useState<LabelValueSelectOption[]>([]);
  const [selectedProject, setSelectedProject] = React.useState<LabelValueSelectOption>();
  const [newTokenExpiration, setNewTokenExpiration] = React.useState<TokenExpiration>(
    TokenExpiration.OneMonth,
  );
  const [tokenExpirationOptions, setTokenExpirationOptions] =
    React.useState<{ value: TokenExpiration; label: string }[]>(EXPIRATION_OPTIONS);

  const { mutateAsync: generate, isLoading: generating } = useGenerateTokenMutation();

  const tokenTypeOptions = React.useMemo(() => {
    const value = [{ label: translate('users.tokens', TokenType.User), value: TokenType.User }];

    if (hasGlobalPermission(currentUser, Permissions.Scan)) {
      value.unshift({
        label: translate('users.tokens', TokenType.Global),
        value: TokenType.Global,
      });
    }
    if (!isEmpty(projects)) {
      value.unshift({
        label: translate('users.tokens', TokenType.Project),
        value: TokenType.Project,
      });
    }

    return value;
  }, [projects, currentUser]);

  React.useEffect(() => {
    if (tokenTypeOptions.length === 1) {
      setNewTokenType(tokenTypeOptions[0].value);
    }
  }, [tokenTypeOptions]);

  React.useEffect(() => {
    getAvailableExpirationOptions()
      .then((options) => {
        setTokenExpirationOptions(options);
      })
      .catch(() => {});

    if (displayTokenTypeInput) {
      getScannableProjects()
        .then(({ projects: projectArray }) => {
          const projects = projectArray.map((project) => ({
            label: project.name,
            value: project.key,
          }));
          setProjects(projects);
          setSelectedProject(projects.length === 1 ? projects[0] : undefined);
        })
        .catch(() => {});
    }
  }, [displayTokenTypeInput, currentUser]);

  const handleGenerateToken = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();

    generate({
      name: newTokenName,
      login,
      type: newTokenType,
      ...(newTokenType === TokenType.Project &&
        selectedProject !== undefined && {
          projectKey: selectedProject.value,
          projectName: selectedProject.label,
        }),
      ...(newTokenExpiration !== TokenExpiration.NoExpiration && {
        expirationDate: computeTokenExpirationDate(newTokenExpiration),
      }),
    })
      .then((newToken) => {
        setNewToken(newToken);
        setNewTokenName('');
        setSelectedProject(undefined);
        setNewTokenType(tokenTypeOptions.length === 1 ? tokenTypeOptions[0].value : undefined);
        setNewTokenExpiration(TokenExpiration.OneMonth);
      })
      .catch(() => {});
  };

  const isSubmitButtonDisabled = () => {
    if (!displayTokenTypeInput) {
      return generating || newTokenName.length <= 0;
    }

    if (generating || newTokenName.length <= 0) {
      return true;
    }
    if (newTokenType === TokenType.Project) {
      return !selectedProject?.value;
    }

    return !newTokenType;
  };

  const handleNewTokenChange = (evt: React.SyntheticEvent<HTMLInputElement>) => {
    setNewTokenName(evt.currentTarget.value);
  };

  const handleNewTokenTypeChange = ({ value }: { value: TokenType }) => {
    setNewTokenType(value);
  };

  const handleProjectChange = (selectedProject: LabelValueSelectOption) => {
    setSelectedProject(selectedProject);
  };

  const handleNewTokenExpirationChange = ({ value }: { value: TokenExpiration }) => {
    setNewTokenExpiration(value);
  };

  const customSpinner = (
    <tr>
      <td>
        <i className="spinner" />
      </td>
    </tr>
  );

  return (
    <>
      <h3 className="spacer-bottom">{translate('users.tokens.generate')}</h3>
      <form autoComplete="off" className="display-flex-center" onSubmit={handleGenerateToken}>
        <div className="display-flex-column input-large spacer-right ">
          <label htmlFor="token-name" className="text-bold">
            {translate('users.tokens.name')}
          </label>
          <input
            id="token-name"
            className="spacer-top it__token-name"
            maxLength={100}
            onChange={handleNewTokenChange}
            placeholder={translate('users.tokens.enter_name')}
            required
            type="text"
            value={newTokenName}
          />
        </div>
        {displayTokenTypeInput && (
          <>
            <div className="display-flex-column input-large spacer-right">
              <label htmlFor="token-select-type" className="text-bold">
                {translate('users.tokens.type')}
              </label>
              <Select
                inputId="token-select-type"
                className="spacer-top it__token-type"
                isSearchable={false}
                onChange={handleNewTokenTypeChange}
                options={tokenTypeOptions}
                placeholder={translate('users.tokens.select_type')}
                value={
                  newTokenType
                    ? tokenTypeOptions.find((option) => option.value === newTokenType)
                    : null
                }
              />
            </div>
            {newTokenType === TokenType.Project && (
              <div className="input-large spacer-right display-flex-column">
                <label htmlFor="token-select-project" className="text-bold">
                  {translate('users.tokens.project')}
                </label>
                <Select
                  inputId="token-select-project"
                  className="spacer-top it__project"
                  onChange={handleProjectChange}
                  options={projects}
                  placeholder={translate('users.tokens.select_project')}
                  value={selectedProject}
                />
              </div>
            )}
          </>
        )}
        <div className="display-flex-column input-medium spacer-right ">
          <label htmlFor="token-select-expiration" className="text-bold">
            {translate('users.tokens.expires_in')}
          </label>
          <Select
            inputId="token-select-expiration"
            className="spacer-top"
            isSearchable={false}
            onChange={handleNewTokenExpirationChange}
            options={tokenExpirationOptions}
            value={tokenExpirationOptions.find((option) => option.value === newTokenExpiration)}
          />
        </div>
        <SubmitButton
          className="it__generate-token"
          style={{ marginTop: 'auto' }}
          disabled={isSubmitButtonDisabled()}
        >
          {translate('users.generate')}
        </SubmitButton>
      </form>
      {newToken && <TokensFormNewToken token={newToken} />}

      <table className="data zebra big-spacer-top fixed">
        <thead>
          <tr>
            <th>{translate('name')}</th>
            <th>{translate('my_account.token_type')}</th>
            <th>{translate('my_account.project_name')}</th>
            <th>{translate('my_account.tokens_last_usage')}</th>
            <th className="text-right">{translate('created')}</th>
            <th className="text-right">{translate('my_account.tokens.expiration')}</th>
            <th className="text-right">{translate('actions')}</th>
          </tr>
        </thead>
        <tbody>
          <Spinner customSpinner={customSpinner} loading={!!loading}>
            {tokens && tokens.length <= 0 ? (
              <tr>
                <td className="note" colSpan={7}>
                  {translate('users.no_tokens')}
                </td>
              </tr>
            ) : (
              tokens?.map((token) => (
                <TokensFormItem
                  deleteConfirmation={deleteConfirmation}
                  key={token.name}
                  login={login}
                  token={token}
                />
              ))
            )}
          </Spinner>
        </tbody>
      </table>
    </>
  );
}

export default withCurrentUserContext(TokensForm);
