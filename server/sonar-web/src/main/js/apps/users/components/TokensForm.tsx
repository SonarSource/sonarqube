/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { generateToken, getTokens } from '../../../api/user-tokens';
import TokensFormItem, { TokenDeleteConfirmation } from './TokensFormItem';
import TokensFormNewToken from './TokensFormNewToken';

interface Props {
  deleteConfirmation: TokenDeleteConfirmation;
  login: string;
  updateTokensCount?: (login: string, tokensCount: number) => void;
}

interface State {
  generating: boolean;
  loading: boolean;
  newToken?: { name: string; token: string };
  newTokenName: string;
  tokens: T.UserToken[];
}

export default class TokensForm extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = {
    generating: false,
    loading: true,
    newTokenName: '',
    tokens: []
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

  updateTokensCount = () => {
    if (this.props.updateTokensCount) {
      this.props.updateTokensCount(this.props.login, this.state.tokens.length);
    }
  };

  handleGenerateToken = (evt: React.SyntheticEvent<HTMLFormElement>) => {
    evt.preventDefault();
    if (this.state.newTokenName.length > 0) {
      this.setState({ generating: true });
      generateToken({ name: this.state.newTokenName, login: this.props.login }).then(
        newToken => {
          if (this.mounted) {
            this.setState(state => {
              const tokens = [
                ...state.tokens,
                { name: newToken.name, createdAt: newToken.createdAt }
              ];
              return { generating: false, newToken, newTokenName: '', tokens };
            }, this.updateTokensCount);
          }
        },
        () => {
          if (this.mounted) {
            this.setState({ generating: false });
          }
        }
      );
    }
  };

  handleRevokeToken = (revokedToken: T.UserToken) => {
    this.setState(
      state => ({
        tokens: state.tokens.filter(token => token.name !== revokedToken.name)
      }),
      this.updateTokensCount
    );
  };

  handleNewTokenChange = (evt: React.SyntheticEvent<HTMLInputElement>) =>
    this.setState({ newTokenName: evt.currentTarget.value });

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
    const { generating, loading, newToken, newTokenName, tokens } = this.state;
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
        <form
          autoComplete="off"
          className="display-flex-center"
          id="generate-token-form"
          onSubmit={this.handleGenerateToken}>
          <input
            className="input-large spacer-right"
            maxLength={100}
            onChange={this.handleNewTokenChange}
            placeholder={translate('users.enter_token_name')}
            required={true}
            type="text"
            value={newTokenName}
          />
          <SubmitButton
            className="js-generate-token"
            disabled={generating || newTokenName.length <= 0}>
            {translate('users.generate')}
          </SubmitButton>
        </form>

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
