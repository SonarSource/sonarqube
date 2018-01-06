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
// @flow
import React from 'react';
import classNames from 'classnames';
import Step from './Step';
import { getTokens, generateToken, revokeToken } from '../../../api/user-tokens';
import AlertErrorIcon from '../../../components/icons-components/AlertErrorIcon';
import { DeleteButton } from '../../../components/ui/buttons';
import { translate } from '../../../helpers/l10n';

/*::
type Props = {|
  currentUser: { login: string },
  finished: boolean,
  open: boolean,
  onContinue: (token: string) => void,
  onOpen: () => void,
  stepNumber: number
|};
*/

/*::
type State = {
  canUseExisting: boolean,
  existingToken: string,
  loading: boolean,
  selection: string,
  tokenName?: string,
  token?: string
};
*/

export default class TokenStep extends React.PureComponent {
  /*:: mounted: boolean; */
  /*:: props: Props; */
  state /*: State */ = {
    canUseExisting: false,
    existingToken: '',
    loading: false,
    selection: 'generate'
  };

  componentDidMount() {
    this.mounted = true;
    getTokens(this.props.currentUser.login).then(
      tokens => {
        if (this.mounted) {
          this.setState({ canUseExisting: tokens.length > 0 });
        }
      },
      () => {}
    );
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  getToken = () =>
    this.state.selection === 'generate' ? this.state.token : this.state.existingToken;

  canContinue = () => {
    const { existingToken, selection, token } = this.state;
    const validExistingToken = existingToken.match(/^[a-z0-9]+$/) != null;
    return (
      (selection === 'generate' && token != null) ||
      (selection === 'use-existing' && existingToken && validExistingToken)
    );
  };

  handleTokenNameChange = (event /*: { target: HTMLInputElement } */) => {
    this.setState({ tokenName: event.target.value });
  };

  handleTokenGenerate = (event /*: Event */) => {
    event.preventDefault();
    const { tokenName } = this.state;
    if (tokenName) {
      this.setState({ loading: true });
      generateToken({ name: tokenName }).then(
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

  handleTokenRevoke = () => {
    const { tokenName } = this.state;
    if (tokenName) {
      this.setState({ loading: true });
      revokeToken({ name: tokenName }).then(
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

  handleContinueClick = (event /*: Event */) => {
    event.preventDefault();
    const token = this.getToken();
    if (token) {
      this.props.onContinue(token);
    }
  };

  handleGenerateClick = (event /*: Event */) => {
    event.preventDefault();
    this.setState({ selection: 'generate' });
  };

  handleUseExistingClick = (event /*: Event */) => {
    event.preventDefault();
    this.setState({ selection: 'use-existing' });
  };

  handleExisingTokenChange = (event /*: { currentTarget: HTMLInputElement } */) => {
    this.setState({ existingToken: event.currentTarget.value });
  };

  renderGenerateOption = () => (
    <div>
      {this.state.canUseExisting ? (
        <a
          className="js-new link-base-color link-no-underline"
          href="#"
          onClick={this.handleGenerateClick}>
          <i
            className={classNames('icon-radio', 'spacer-right', {
              'is-checked': this.state.selection === 'generate'
            })}
          />
          {translate('onboading.token.generate_token')}
        </a>
      ) : (
        translate('onboading.token.generate_token')
      )}
      {this.state.selection === 'generate' && (
        <div className="big-spacer-top">
          <form onSubmit={this.handleTokenGenerate}>
            <input
              autoFocus={true}
              className="input-large spacer-right text-middle"
              onChange={this.handleTokenNameChange}
              placeholder={translate('onboading.token.generate_token.placeholder')}
              required={true}
              type="text"
              value={this.state.tokenName || ''}
            />
            {this.state.loading ? (
              <i className="spinner text-middle" />
            ) : (
              <button className="text-middle" disabled={!this.state.tokenName}>
                {translate('onboarding.token.generate')}
              </button>
            )}
          </form>
        </div>
      )}
    </div>
  );

  renderUseExistingOption = () => {
    const { existingToken } = this.state;
    const validInput = !existingToken || existingToken.match(/^[a-z0-9]+$/) != null;

    return (
      <div className="big-spacer-top">
        <a
          className="js-new link-base-color link-no-underline"
          href="#"
          onClick={this.handleUseExistingClick}>
          <i
            className={classNames('icon-radio', 'spacer-right', {
              'is-checked': this.state.selection === 'use-existing'
            })}
          />
          {translate('onboarding.token.use_existing_token')}
        </a>
        {this.state.selection === 'use-existing' && (
          <div className="big-spacer-top">
            <input
              autoFocus={true}
              className="input-large spacer-right text-middle"
              onChange={this.handleExisingTokenChange}
              placeholder={translate('onboarding.token.use_existing_token.placeholder')}
              required={true}
              type="text"
              value={this.state.existingToken}
            />
            {!validInput && (
              <span className="text-danger">
                <AlertErrorIcon className="little-spacer-right text-text-top" />
                {translate('onboarding.token.invalid_format')}
              </span>
            )}
          </div>
        )}
      </div>
    );
  };

  renderForm = () => {
    const { canUseExisting, loading, token, tokenName } = this.state;

    return (
      <div className="boxed-group-inner">
        {token != null ? (
          <form onSubmit={this.handleTokenRevoke}>
            <span className="text-middle">
              {tokenName}
              {': '}
            </span>
            <strong className="spacer-right text-middle">{token}</strong>
            {loading ? (
              <i className="spinner text-middle" />
            ) : (
              <DeleteButton className="button-small text-middle" onClick={this.handleTokenRevoke} />
            )}
          </form>
        ) : (
          <div>
            {this.renderGenerateOption()}
            {canUseExisting && this.renderUseExistingOption()}
          </div>
        )}

        <div className="note big-spacer-top width-50">{translate('onboarding.token.text')}</div>

        {this.canContinue() && (
          <div className="big-spacer-top">
            <button className="js-continue" onClick={this.handleContinueClick}>
              {translate('continue')}
            </button>
          </div>
        )}
      </div>
    );
  };

  renderResult = () => {
    const { selection, tokenName } = this.state;
    const token = this.getToken();

    if (!token) {
      return null;
    }

    return (
      <div className="boxed-group-actions">
        <i className="icon-check spacer-right" />
        {selection === 'generate' && tokenName && `${tokenName}: `}
        <strong>{token}</strong>
      </div>
    );
  };

  render() {
    return (
      <Step
        finished={this.props.finished}
        onOpen={this.props.onOpen}
        open={this.props.open}
        renderForm={this.renderForm}
        renderResult={this.renderResult}
        stepNumber={this.props.stepNumber}
        stepTitle={translate('onboarding.token.header')}
      />
    );
  }
}
