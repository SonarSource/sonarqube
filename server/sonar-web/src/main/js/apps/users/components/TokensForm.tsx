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
import { getScannableProjects } from '../../../api/components';
import { generateToken, getTokens } from '../../../api/user-tokens';
import withCurrentUserContext from '../../../app/components/current-user/withCurrentUserContext';
import { SubmitButton } from '../../../components/controls/buttons';
import Select, { BasicSelectOption } from '../../../components/controls/Select';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate } from '../../../helpers/l10n';
import { hasGlobalPermission } from '../../../helpers/users';
import { Permissions } from '../../../types/permissions';
import { TokenType, UserToken } from '../../../types/token';
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
  selectedProjectkey?: string;
}

export class TokensForm extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    generating: false,
    loading: true,
    newTokenName: '',
    newTokenType: this.props.displayTokenTypeInput ? undefined : TokenType.User,
    selectedProjectkey: '',
    tokens: [],
    projects: []
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchTokens();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchTokens = () => {
    this.setState({ loading: true });
    getTokens(this.props.login).then(
      tokens => {
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

  fetchProjects = async () => {
    const { projects: projectArray } = await getScannableProjects();
    const projects = projectArray.map(project => ({ label: project.name, value: project.key }));
    this.setState({
      projects
    });
  };

  updateTokensCount = () => {
    if (this.props.updateTokensCount) {
      this.props.updateTokensCount(this.props.login, this.state.tokens.length);
    }
  };

  handleGenerateToken = async (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    const { login } = this.props;
    const { newTokenName, newTokenType, selectedProjectkey } = this.state;
    this.setState({ generating: true });

    try {
      const newToken = await generateToken({
        name: newTokenName,
        login,
        type: newTokenType,
        ...(newTokenType === TokenType.Project && { projectKey: selectedProjectkey })
      });

      if (this.mounted) {
        this.setState(state => {
          const tokens = [...state.tokens, { name: newToken.name, createdAt: newToken.createdAt }];
          return {
            generating: false,
            newToken,
            newTokenName: '',
            selectedProjectkey: '',
            newTokenType: undefined,
            tokens
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
      state => ({
        tokens: state.tokens.filter(token => token.name !== revokedToken.name)
      }),
      this.updateTokensCount
    );
  };

  isSubmitButtonDisabled = () => {
    const { displayTokenTypeInput } = this.props;
    const { generating, newTokenName, newTokenType, selectedProjectkey } = this.state;

    if (!displayTokenTypeInput) {
      return generating || newTokenName.length <= 0;
    }

    if (generating || newTokenName.length <= 0) {
      return true;
    }
    if (newTokenType === TokenType.Project) {
      return !selectedProjectkey;
    }

    return !newTokenType;
  };

  handleNewTokenChange = (evt: React.SyntheticEvent<HTMLInputElement>) => {
    this.setState({ newTokenName: evt.currentTarget.value });
  };

  handleNewTokenTypeChange = ({ value }: { value: TokenType }) => {
    if (value === TokenType.Project && this.state.projects.length === 0) {
      this.fetchProjects();
    }
    this.setState({ newTokenType: value });
  };

  handleProjectChange = ({ value }: { value: string }) => {
    this.setState({ selectedProjectkey: value });
  };

  renderForm() {
    const { newTokenName, newTokenType, projects, selectedProjectkey } = this.state;
    const { displayTokenTypeInput, currentUser } = this.props;

    const tokenTypeOptions = [
      { label: translate('users.tokens', TokenType.Project), value: TokenType.Project },
      { label: translate('users.tokens', TokenType.User), value: TokenType.User }
    ];
    if (hasGlobalPermission(currentUser, Permissions.Scan)) {
      tokenTypeOptions.push({
        label: translate('users.tokens', TokenType.Global),
        value: TokenType.Global
      });
    }

    return (
      <form autoComplete="off" className="display-flex-center" onSubmit={this.handleGenerateToken}>
        <input
          className="input-large spacer-right it__token-name"
          maxLength={100}
          onChange={this.handleNewTokenChange}
          placeholder={translate('users.enter_token_name')}
          required={true}
          type="text"
          value={newTokenName}
        />
        {displayTokenTypeInput && (
          <>
            <Select
              className="input-medium spacer-right it__token-type"
              isSearchable={false}
              onChange={this.handleNewTokenTypeChange}
              options={tokenTypeOptions}
              placeholder={translate('users.select_token_type')}
              value={tokenTypeOptions.find(option => option.value === newTokenType) || null}
            />
            {newTokenType === TokenType.Project && (
              <Select
                className="input-medium spacer-right it__project"
                onChange={this.handleProjectChange}
                options={projects}
                placeholder={translate('users.select_token_project')}
                value={projects.find(project => project.value === selectedProjectkey)}
              />
            )}
          </>
        )}

        <SubmitButton className="it__generate-token" disabled={this.isSubmitButtonDisabled()}>
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
          <td className="note" colSpan={3}>
            {translate('users.no_tokens')}
          </td>
        </tr>
      );
    }
    return tokens.map(token => (
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
        <h3 className="spacer-bottom">{translate('users.generate_tokens')}</h3>
        {this.renderForm()}
        {newToken && <TokensFormNewToken token={newToken} />}

        <table className="data zebra big-spacer-top">
          <thead>
            <tr>
              <th>{translate('name')}</th>
              <th>{translate('my_account.tokens_last_usage')}</th>
              <th className="text-right">{translate('created')}</th>
              <th />
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
