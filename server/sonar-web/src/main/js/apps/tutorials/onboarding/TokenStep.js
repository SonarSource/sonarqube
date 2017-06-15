/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';
import Step from './Step';
import { generateToken, revokeToken } from '../../../api/user-tokens';
import { translate } from '../../../helpers/l10n';

type Props = {
  open: boolean,
  onContinue: (token: string) => void,
  stepNumber: number
};

type State = {
  loading: boolean,
  tokenName?: string,
  token?: string
};

export default class TokenStep extends React.PureComponent {
  mounted: boolean;
  props: Props;

  static defaultProps = {
    stepNumber: 1
  };

  state: State = {
    loading: false
  };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleTokenNameChange = (event: { target: HTMLInputElement }) => {
    this.setState({ tokenName: event.target.value });
  };

  handleTokenGenerate = (event: Event) => {
    event.preventDefault();
    const { tokenName } = this.state;
    if (tokenName) {
      this.setState({ loading: true });
      generateToken(tokenName).then(
        ({ token }) => {
          if (this.mounted) {
            this.setState({ loading: false, token });
          }
        },
        () => {
          if (this.mounted) {
            this.setState({ loading: false });
          }
        }
      );
    }
  };

  handleTokenRevoke = (event: Event) => {
    event.preventDefault();
    const { tokenName } = this.state;
    if (tokenName) {
      this.setState({ loading: true });
      revokeToken(tokenName).then(
        () => {
          if (this.mounted) {
            this.setState({ loading: false, token: undefined, tokenName: undefined });
          }
        },
        () => {
          if (this.mounted) {
            this.setState({ loading: false });
          }
        }
      );
    }
  };

  handleContinueClick = (event: Event) => {
    event.preventDefault();
    if (this.state.token) {
      this.props.onContinue(this.state.token);
    }
  };

  renderForm = () => {
    const { loading, token, tokenName } = this.state;

    return (
      <div className="boxed-group-inner">
        <div className="big-spacer-bottom width-50">
          {translate('onboarding.token.text')}
        </div>

        {token != null
          ? <form onSubmit={this.handleTokenRevoke}>
              {tokenName}{': '}
              <span className="monospaced spacer-right">{token}</span>
              {loading
                ? <i className="spinner" />
                : <button className="button-clean" onClick={this.handleTokenRevoke}>
                    <i className="icon-delete" />
                  </button>}
            </form>
          : <form onSubmit={this.handleTokenGenerate}>
              <input
                autoFocus={true}
                className="input-large spacer-right"
                onChange={this.handleTokenNameChange}
                placeholder={translate('onboarding.token.placeholder')}
                required={true}
                type="text"
                value={tokenName || ''}
              />
              {loading
                ? <i className="spinner" />
                : <button>{translate('onboarding.token.generate')}</button>}
            </form>}

        {token != null &&
          <div className="big-spacer-top">
            <button className="js-continue" onClick={this.handleContinueClick}>
              {translate('continue')}
            </button>
          </div>}
      </div>
    );
  };

  renderResult = () => {
    const { token, tokenName } = this.state;

    if (!token) {
      return null;
    }

    return (
      <div className="boxed-group-actions">
        <i className="icon-check spacer-right" />
        {tokenName}{': '}
        <strong className="monospaced">{token}</strong>
      </div>
    );
  };

  render() {
    return (
      <Step
        open={this.props.open}
        renderForm={this.renderForm}
        renderResult={this.renderResult}
        stepNumber={this.props.stepNumber}
        stepTitle={translate('onboarding.token.header')}
      />
    );
  }
}
