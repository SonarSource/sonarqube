/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import TokensFormItem from './TokensFormItem';
import TokensFormNewToken from './TokensFormNewToken';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import { getTokens, generateToken, UserToken } from '../../../api/user-tokens';
import { translate } from '../../../helpers/l10n';

interface Props {
  login: string;
  updateTokensCount?: (login: string, tokensCount: number) => void;
}

interface State {
  generating: boolean;
  loading: boolean;
  newToken?: { name: string; token: string };
  newTokenName: string;
  tokens: UserToken[];
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

  handleRevokeToken = (revokedToken: UserToken) => {
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
          <td colSpan={3} className="note">
            {translate('users.no_tokens')}
          </td>
        </tr>
      );
    }
    return tokens.map(token => (
      <TokensFormItem
        key={token.name}
        login={this.props.login}
        token={token}
        onRevokeToken={this.handleRevokeToken}
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
        <form id="generate-token-form" onSubmit={this.handleGenerateToken} autoComplete="off">
          <input
            className="spacer-right"
            type="text"
            maxLength={100}
            onChange={this.handleNewTokenChange}
            placeholder={translate('users.enter_token_name')}
            required={true}
            value={newTokenName}
          />
          <button
            className="js-generate-token"
            disabled={generating || newTokenName.length <= 0}
            type="submit">
            {translate('users.generate')}
          </button>
        </form>

        {newToken && <TokensFormNewToken token={newToken} />}

        <table className="data zebra big-spacer-top ">
          <thead>
            <tr>
              <th>{translate('name')}</th>
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
