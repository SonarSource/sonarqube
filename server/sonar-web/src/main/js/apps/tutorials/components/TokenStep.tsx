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
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import * as classNames from 'classnames';
import Step from './Step';
import AlertErrorIcon from '../../../components/icons-components/AlertErrorIcon';
import AlertSuccessIcon from '../../../components/icons-components/AlertSuccessIcon';
import { DeleteButton, SubmitButton, Button } from '../../../components/ui/buttons';
import { getTokens, generateToken, revokeToken } from '../../../api/user-tokens';
import { translate } from '../../../helpers/l10n';

interface Props {
  currentUser: { login: string };
  finished: boolean;
  initialTokenName?: string;
  open: boolean;
  onContinue: (token: string) => void;
  onOpen: () => void;
  stepNumber: number;
}

interface State {
  existingToken: string;
  loading: boolean;
  selection: string;
  tokenName?: string;
  token?: string;
  tokens?: T.UserToken[];
}

export default class TokenStep extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      existingToken: '',
      loading: false,
      selection: 'generate',
      tokenName: props.initialTokenName
    };
  }

  componentDidMount() {
    this.mounted = true;
    getTokens(this.props.currentUser.login).then(
      tokens => {
        if (this.mounted) {
          this.setState({ tokens });
          if (
            this.props.initialTokenName !== undefined &&
            this.props.initialTokenName === this.state.tokenName
          ) {
            this.setState({ tokenName: this.getUniqueTokenName(tokens) });
          }
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

  getUniqueTokenName = (tokens: T.UserToken[]) => {
    const { initialTokenName = '' } = this.props;
    const hasToken = (name: string) => tokens.find(token => token.name === name) !== undefined;

    if (!hasToken(initialTokenName)) {
      return initialTokenName;
    }

    let i = 1;
    while (hasToken(`${initialTokenName} ${i}`)) {
      i++;
    }
    return `${initialTokenName} ${i}`;
  };

  canContinue = () => {
    const { existingToken, selection, token } = this.state;
    const validExistingToken = existingToken.match(/^[a-z0-9]+$/) != null;
    return (
      (selection === 'generate' && token != null) ||
      (selection === 'use-existing' && existingToken && validExistingToken)
    );
  };

  handleTokenNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ tokenName: event.target.value });
  };

  handleTokenGenerate = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const { tokenName } = this.state;
    if (tokenName) {
      this.setState({ loading: true });
      generateToken({ name: tokenName }).then(({ token }) => {
        if (this.mounted) {
          this.setState({ loading: false, token });
        }
      }, this.stopLoading);
    }
  };

  handleTokenRevoke = () => {
    const { tokenName } = this.state;
    if (tokenName) {
      this.setState({ loading: true });
      revokeToken({ name: tokenName }).then(() => {
        if (this.mounted) {
          this.setState({ loading: false, token: undefined, tokenName: undefined });
        }
      }, this.stopLoading);
    }
  };

  handleContinueClick = () => {
    const token = this.getToken();
    if (token) {
      this.props.onContinue(token);
    }
  };

  handleGenerateClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.setState({ selection: 'generate' });
  };

  handleUseExistingClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.setState({ selection: 'use-existing' });
  };

  handleExisingTokenChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ existingToken: event.currentTarget.value });
  };

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  renderGenerateOption = () => (
    <div>
      {this.state.tokens !== undefined && this.state.tokens.length > 0 ? (
        <a
          className="js-new link-base-color link-no-underline"
          href="#"
          onClick={this.handleGenerateClick}>
          <i
            className={classNames('icon-radio', 'spacer-right', {
              'is-checked': this.state.selection === 'generate'
            })}
          />
          {translate('onboarding.token.generate_token')}
        </a>
      ) : (
        translate('onboarding.token.generate_token')
      )}
      {this.state.selection === 'generate' && (
        <div className="big-spacer-top">
          <form onSubmit={this.handleTokenGenerate}>
            <input
              autoFocus={true}
              className="input-super-large spacer-right text-middle"
              onChange={this.handleTokenNameChange}
              placeholder={translate('onboarding.token.generate_token.placeholder')}
              required={true}
              type="text"
              value={this.state.tokenName || ''}
            />
            {this.state.loading ? (
              <i className="spinner text-middle" />
            ) : (
              <SubmitButton className="text-middle" disabled={!this.state.tokenName}>
                {translate('onboarding.token.generate')}
              </SubmitButton>
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
              className="input-super-large spacer-right text-middle"
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
    const { loading, token, tokenName, tokens } = this.state;
    const canUseExisting = tokens !== undefined && tokens.length > 0;

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

        <div className="note big-spacer-top width-50">
          <FormattedMessage
            defaultMessage={translate('onboarding.token.text')}
            id="onboarding.token.text"
            values={{
              link: (
                <Link target="_blank" to="/account/security">
                  {translate('onboarding.token.text.user_account')}
                </Link>
              )
            }}
          />
        </div>

        {this.canContinue() && (
          <div className="big-spacer-top">
            <Button className="js-continue" onClick={this.handleContinueClick}>
              {translate('continue')}
            </Button>
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
      <div className="boxed-group-actions display-flex-center">
        <AlertSuccessIcon className="spacer-right" />
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
