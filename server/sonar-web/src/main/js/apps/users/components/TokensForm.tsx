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
import { generateToken, getTokens } from '../../../api/user-tokens';
import withCurrentUserContext from '../../../app/components/current-user/withCurrentUserContext';
import { SubmitButton } from '../../../components/controls/buttons';
import Select, { BasicSelectOption } from '../../../components/controls/Select';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate } from '../../../helpers/l10n';
import {
  computeTokenExpirationDate,
  EXPIRATION_OPTIONS,
  getAvailableExpirationOptions,
} from '../../../helpers/tokens';
import { hasGlobalPermission } from '../../../helpers/users';
import { Permissions } from '../../../types/permissions';
import { TokenExpiration, TokenType, UserToken } from '../../../types/token';
import { CurrentUser } from '../../../types/users';
import TokensFormItem, { TokenDeleteConfirmation } from './TokensFormItem';
import TokensFormNewToken from './TokensFormNewToken';

interface Props {
  deleteConfirmation: TokenDeleteConfirmation;
  login: string;
  updateTokensCount?: (login: string, tokensCount: number) => void;
  displayTokenTypeInput: boolean;
  currentUser: CurrentUser;
}

interface State {
  generating: boolean;
  loading: boolean;
  newToken?: { name: string; token: string };
  newTokenName: string;
  newTokenType?: TokenType;
  tokens: UserToken[];
  projects: BasicSelectOption[];
  selectedProject?: BasicSelectOption;
  newTokenExpiration: TokenExpiration;
  tokenExpirationOptions: { value: TokenExpiration; label: string }[];
  tokenTypeOptions: Array<{ label: string; value: TokenType }>;
}

export class TokensForm extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    generating: false,
    loading: true,
    newTokenName: '',
    newTokenType: this.props.displayTokenTypeInput ? undefined : TokenType.User,
    tokens: [],
    projects: [],
    newTokenExpiration: TokenExpiration.OneMonth,
    tokenExpirationOptions: EXPIRATION_OPTIONS,
    tokenTypeOptions: [],
  };

  componentDidMount() {
    this.mounted = true;
    this.loadData();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadData = async () => {
    this.fetchTokens();
    this.fetchTokenSettings();

    if (this.props.displayTokenTypeInput) {
      const projects = await this.fetchProjects();
      this.constructTokenTypeOptions(projects);
    }
  };

  fetchTokens = () => {
    this.setState({ loading: true });
    getTokens(this.props.login).then(
      (tokens) => {
        if (this.mounted) {
          this.setState({ loading: false, tokens });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  fetchTokenSettings = async () => {
    const tokenExpirationOptions = await getAvailableExpirationOptions();
    if (this.mounted) {
      this.setState({ tokenExpirationOptions });
    }
  };

  fetchProjects = async () => {
    const { projects: projectArray } = await getScannableProjects();
    const projects = projectArray.map((project) => ({ label: project.name, value: project.key }));

    this.setState({
      projects,
      selectedProject: projects.length === 1 ? projects[0] : undefined,
    });

    return projects;
  };

  constructTokenTypeOptions = (projects: BasicSelectOption[]) => {
    const { currentUser } = this.props;

    const tokenTypeOptions = [
      { label: translate('users.tokens', TokenType.User), value: TokenType.User },
    ];
    if (hasGlobalPermission(currentUser, Permissions.Scan)) {
      tokenTypeOptions.unshift({
        label: translate('users.tokens', TokenType.Global),
        value: TokenType.Global,
      });
    }
    if (!isEmpty(projects)) {
      tokenTypeOptions.unshift({
        label: translate('users.tokens', TokenType.Project),
        value: TokenType.Project,
      });
    }

    if (tokenTypeOptions.length === 1) {
      this.setState({
        newTokenType: tokenTypeOptions[0].value,
        tokenTypeOptions,
      });
    } else {
      this.setState({ tokenTypeOptions });
    }
  };

  updateTokensCount = () => {
    if (this.props.updateTokensCount) {
      this.props.updateTokensCount(this.props.login, this.state.tokens.length);
    }
  };

  handleGenerateToken = async (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    const { login } = this.props;
    const {
      newTokenName,
      newTokenType = TokenType.User,
      selectedProject,
      tokenTypeOptions,
      newTokenExpiration,
    } = this.state;
    this.setState({ generating: true });

    try {
      const newToken = await generateToken({
        name: newTokenName,
        login,
        type: newTokenType,
        ...(newTokenType === TokenType.Project &&
          selectedProject !== undefined && { projectKey: selectedProject.value }),
        ...(newTokenExpiration !== TokenExpiration.NoExpiration && {
          expirationDate: computeTokenExpirationDate(newTokenExpiration),
        }),
      });

      if (this.mounted) {
        this.setState((state) => {
          const tokens: UserToken[] = [
            ...state.tokens,
            {
              name: newToken.name,
              createdAt: newToken.createdAt,
              isExpired: false,
              expirationDate: newToken.expirationDate,
              type: newTokenType,
              ...(newTokenType === TokenType.Project &&
                selectedProject !== undefined && {
                  project: { key: selectedProject.value, name: selectedProject.label },
                }),
            },
          ];
          return {
            generating: false,
            newToken,
            newTokenName: '',
            selectedProject: undefined,
            newTokenType: tokenTypeOptions.length === 1 ? tokenTypeOptions[0].value : undefined,
            newTokenExpiration: TokenExpiration.OneMonth,
            tokens,
          };
        }, this.updateTokensCount);
      }
    } catch (e) {
      if (this.mounted) {
        this.setState({ generating: false });
      }
    }
  };

  handleRevokeToken = (revokedToken: UserToken) => {
    this.setState(
      (state) => ({
        tokens: state.tokens.filter((token) => token.name !== revokedToken.name),
      }),
      this.updateTokensCount
    );
  };

  isSubmitButtonDisabled = () => {
    const { displayTokenTypeInput } = this.props;
    const { generating, newTokenName, newTokenType, selectedProject } = this.state;

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

  handleNewTokenChange = (evt: React.SyntheticEvent<HTMLInputElement>) => {
    this.setState({ newTokenName: evt.currentTarget.value });
  };

  handleNewTokenTypeChange = ({ value }: { value: TokenType }) => {
    this.setState({ newTokenType: value });
  };

  handleProjectChange = (selectedProject: BasicSelectOption) => {
    this.setState({ selectedProject });
  };

  handleNewTokenExpirationChange = ({ value }: { value: TokenExpiration }) => {
    this.setState({ newTokenExpiration: value });
  };

  renderForm() {
    const {
      newTokenName,
      newTokenType,
      projects,
      selectedProject,
      newTokenExpiration,
      tokenExpirationOptions,
      tokenTypeOptions,
    } = this.state;
    const { displayTokenTypeInput } = this.props;

    return (
      <form autoComplete="off" className="display-flex-center" onSubmit={this.handleGenerateToken}>
        <div className="display-flex-column input-large spacer-right ">
          <label htmlFor="token-name" className="text-bold">
            {translate('users.tokens.name')}
          </label>
          <input
            id="token-name"
            className="spacer-top it__token-name"
            maxLength={100}
            onChange={this.handleNewTokenChange}
            placeholder={translate('users.tokens.enter_name')}
            required={true}
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
                id="token-select-type"
                className="spacer-top it__token-type"
                isSearchable={false}
                onChange={this.handleNewTokenTypeChange}
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
                  id="token-select-project"
                  className="spacer-top it__project"
                  onChange={this.handleProjectChange}
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
            id="token-select-expiration"
            className="spacer-top"
            isSearchable={false}
            onChange={this.handleNewTokenExpirationChange}
            options={tokenExpirationOptions}
            value={tokenExpirationOptions.find((option) => option.value === newTokenExpiration)}
          />
        </div>
        <SubmitButton
          className="it__generate-token"
          style={{ marginTop: 'auto' }}
          disabled={this.isSubmitButtonDisabled()}
        >
          {translate('users.generate')}
        </SubmitButton>
      </form>
    );
  }

  renderItems() {
    const { tokens } = this.state;
    if (tokens.length <= 0) {
      return (
        <tr>
          <td className="note" colSpan={7}>
            {translate('users.no_tokens')}
          </td>
        </tr>
      );
    }
    return tokens.map((token) => (
      <TokensFormItem
        deleteConfirmation={this.props.deleteConfirmation}
        key={token.name}
        login={this.props.login}
        onRevokeToken={this.handleRevokeToken}
        token={token}
      />
    ));
  }

  render() {
    const { loading, newToken, tokens } = this.state;
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
        {this.renderForm()}
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
              <th aria-label={translate('actions')} />
            </tr>
          </thead>
          <tbody>
            <DeferredSpinner customSpinner={customSpinner} loading={loading && tokens.length <= 0}>
              {this.renderItems()}
            </DeferredSpinner>
          </tbody>
        </table>
      </>
    );
  }
}

export default withCurrentUserContext(TokensForm);
