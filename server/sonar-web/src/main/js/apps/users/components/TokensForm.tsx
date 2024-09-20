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

import { Button, ButtonVariety, Heading, Label, Spinner } from '@sonarsource/echoes-react';
import {
  ContentCell,
  GreySeparator,
  InputField,
  InputSelect,
  Table,
  TableRow,
} from 'design-system';
import { isEmpty } from 'lodash';
import * as React from 'react';
import { getScannableProjects } from '../../../api/components';
import withCurrentUserContext from '../../../app/components/current-user/withCurrentUserContext';
import { translate } from '../../../helpers/l10n';
import { LabelValueSelectOption } from '../../../helpers/search';
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
  currentUser: CurrentUser;
  deleteConfirmation: TokenDeleteConfirmation;
  displayTokenTypeInput: boolean;
  login: string;
}

const COLUMN_WIDTHS = ['auto', 'auto', 'auto', 'auto', 'auto', 'auto', '5%'];

export function TokensForm(props: Readonly<Props>) {
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
    React.useState<{ label: string; value: TokenExpiration }[]>(EXPIRATION_OPTIONS);

  const { mutateAsync: generate, isPending: generating } = useGenerateTokenMutation();

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
      login,
      name: newTokenName,
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

  const handleNewTokenTypeChange = (newTokenType: { value: unknown } | null) => {
    setNewTokenType(newTokenType?.value as TokenType);
  };

  const handleProjectChange = (selectedProject: LabelValueSelectOption) => {
    setSelectedProject(selectedProject);
  };

  const handleNewTokenExpirationChange = (newTokenExpiration: { value: unknown } | null) => {
    setNewTokenExpiration(newTokenExpiration?.value as TokenExpiration);
  };

  const tableHeader = (
    <TableRow>
      <ContentCell>{translate('name')}</ContentCell>
      <ContentCell>{translate('my_account.token_type')}</ContentCell>
      <ContentCell>{translate('my_account.project_name')}</ContentCell>
      <ContentCell>{translate('my_account.tokens_last_usage')}</ContentCell>
      <ContentCell>{translate('created')}</ContentCell>
      <ContentCell>{translate('my_account.tokens.expiration')}</ContentCell>
    </TableRow>
  );

  return (
    <>
      <GreySeparator className="sw-mb-4 sw-mt-6" />

      <Heading as="h2" hasMarginBottom>
        {translate('users.tokens.generate')}
      </Heading>

      <form autoComplete="off" className="sw-flex sw-items-center" onSubmit={handleGenerateToken}>
        <div className="sw-flex sw-flex-col sw-mr-2">
          <Label htmlFor="token-name">{translate('users.tokens.name')}</Label>

          <InputField
            className="sw-mt-2 sw-w-auto it__token-name"
            id="token-name"
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
            <div className="sw-flex sw-flex-col sw-mr-2">
              <Label htmlFor="token-select-type">{translate('users.tokens.type')}</Label>

              <InputSelect
                className="sw-mt-2 it__token-type"
                inputId="token-select-type"
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
              <div className="sw-flex sw-flex-col sw-mr-2">
                <Label htmlFor="token-select-project">{translate('users.tokens.project')}</Label>

                <InputSelect
                  className="sw-mt-2 it__project"
                  inputId="token-select-project"
                  onChange={handleProjectChange}
                  options={projects}
                  placeholder={translate('users.tokens.select_project')}
                  value={selectedProject}
                />
              </div>
            )}
          </>
        )}

        <div className="sw-flex sw-flex-col sw-mr-2">
          <Label htmlFor="token-select-expiration">{translate('users.tokens.expires_in')}</Label>

          <InputSelect
            className="sw-mt-2"
            inputId="token-select-expiration"
            isSearchable={false}
            onChange={handleNewTokenExpirationChange}
            options={tokenExpirationOptions}
            value={tokenExpirationOptions.find((option) => option.value === newTokenExpiration)}
          />
        </div>

        <Button
          className="it__generate-token"
          isDisabled={isSubmitButtonDisabled()}
          style={{ marginTop: 'auto' }}
          type="submit"
          variety={ButtonVariety.Primary}
        >
          {translate('users.generate')}
        </Button>
      </form>

      {newToken && <TokensFormNewToken token={newToken} />}

      <GreySeparator className="sw-mb-4 sw-mt-6" />

      <Spinner isLoading={loading}>
        <Table
          className="sw-min-h-40 sw-w-full"
          columnCount={COLUMN_WIDTHS.length}
          columnWidths={COLUMN_WIDTHS}
          header={tableHeader}
          noHeaderTopBorder
        >
          {tokens && tokens.length <= 0 ? (
            <TableRow>
              <ContentCell colSpan={7}>{translate('users.no_tokens')}</ContentCell>
            </TableRow>
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
        </Table>
      </Spinner>
    </>
  );
}

export default withCurrentUserContext(TokensForm);
