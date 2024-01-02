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
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { generateToken, getTokens, revokeToken } from '../../../api/user-tokens';
import { translate } from '../../../helpers/l10n';
import {
  computeTokenExpirationDate,
  EXPIRATION_OPTIONS,
  getAvailableExpirationOptions,
} from '../../../helpers/tokens';
import { TokenExpiration, TokenType, UserToken } from '../../../types/token';
import { LoggedInUser } from '../../../types/users';
import DocumentationTooltip from '../../common/DocumentationTooltip';
import Link from '../../common/Link';
import { Button, DeleteButton, SubmitButton } from '../../controls/buttons';
import Radio from '../../controls/Radio';
import Select from '../../controls/Select';
import AlertErrorIcon from '../../icons/AlertErrorIcon';
import AlertSuccessIcon from '../../icons/AlertSuccessIcon';
import ProjectTokenScopeInfo from '../components/ProjectTokenScopeInfo';
import Step from '../components/Step';
import { getUniqueTokenName } from '../utils';

interface Props {
  currentUser: Pick<LoggedInUser, 'login'>;
  projectKey: string;
  finished: boolean;
  initialTokenName?: string;
  stepNumber: number;
  open: boolean;
  onContinue: (token: string) => void;
  onOpen: VoidFunction;
}

interface State {
  existingToken: string;
  loading: boolean;
  selection: string;
  tokenName?: string;
  token?: string;
  tokens?: UserToken[];
  tokenExpiration: TokenExpiration;
  tokenExpirationOptions: { value: TokenExpiration; label: string }[];
}

const TOKEN_FORMAT_REGEX = /^[_a-z0-9]+$/;

export default class TokenStep extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      existingToken: '',
      loading: false,
      selection: 'generate',
      tokenName: props.initialTokenName,
      tokenExpiration: TokenExpiration.OneMonth,
      tokenExpirationOptions: EXPIRATION_OPTIONS,
    };
  }

  async componentDidMount() {
    this.mounted = true;
    const { currentUser, initialTokenName } = this.props;
    const { tokenName } = this.state;

    const tokenExpirationOptions = await getAvailableExpirationOptions();
    if (tokenExpirationOptions && this.mounted) {
      this.setState({ tokenExpirationOptions });
    }

    const tokens = await getTokens(currentUser.login).catch(() => {
      /* noop */
    });

    if (tokens && this.mounted) {
      this.setState({ tokens });
      if (initialTokenName !== undefined && initialTokenName === tokenName) {
        this.setState({ tokenName: getUniqueTokenName(tokens, initialTokenName) });
      }
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  getToken = () =>
    this.state.selection === 'generate' ? this.state.token : this.state.existingToken;

  canContinue = () => {
    const { existingToken, selection, token } = this.state;
    const validExistingToken = existingToken.match(TOKEN_FORMAT_REGEX) != null;
    return (
      (selection === 'generate' && token != null) ||
      (selection === 'use-existing' && existingToken && validExistingToken)
    );
  };

  handleTokenNameChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ tokenName: event.target.value });
  };

  handleTokenExpirationChange = ({ value }: { value: TokenExpiration }) => {
    this.setState({ tokenExpiration: value });
  };

  handleTokenGenerate = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const { tokenName, tokenExpiration } = this.state;
    const { projectKey } = this.props;

    if (tokenName) {
      this.setState({ loading: true });
      try {
        const { token } = await generateToken({
          name: tokenName,
          type: TokenType.Project,
          projectKey,
          ...(tokenExpiration !== TokenExpiration.NoExpiration && {
            expirationDate: computeTokenExpirationDate(tokenExpiration),
          }),
        });
        if (this.mounted) {
          this.setState({ loading: false, token });
        }
      } catch (e) {
        this.stopLoading();
      }
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

  handleModeChange = (mode: string) => {
    this.setState({ selection: mode });
  };

  handleExisingTokenChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ existingToken: event.currentTarget.value });
  };

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  renderGenerateOption = () => {
    const { loading, selection, tokens, tokenName, tokenExpiration, tokenExpirationOptions } =
      this.state;
    return (
      <div>
        {tokens !== undefined && tokens.length > 0 ? (
          <Radio
            checked={selection === 'generate'}
            onCheck={this.handleModeChange}
            value="generate"
          >
            {translate('onboarding.token.generate', TokenType.Project)}
          </Radio>
        ) : (
          translate('onboarding.token.generate', TokenType.Project)
        )}
        {selection === 'generate' && (
          <div className="big-spacer-top">
            <form className="display-flex-center" onSubmit={this.handleTokenGenerate}>
              <div className="display-flex-column">
                <label className="h3" htmlFor="generate-token-input">
                  {translate('onboarding.token.name.label')}
                  <DocumentationTooltip
                    className="spacer-left"
                    content={translate('onboarding.token.name.help')}
                    links={[
                      {
                        href: '/user-guide/user-account/generating-and-using-tokens/',
                        label: translate('learn_more'),
                      },
                    ]}
                  />
                </label>
                <input
                  id="generate-token-input"
                  autoFocus={true}
                  className="input-super-large spacer-right spacer-top text-middle"
                  onChange={this.handleTokenNameChange}
                  required={true}
                  type="text"
                  value={tokenName || ''}
                />
              </div>
              <div className="display-flex-column spacer-left big-spacer-right">
                <label htmlFor="token-select-expiration" className="h3">
                  {translate('users.tokens.expires_in')}
                </label>
                <div className="display-flex-center">
                  <Select
                    id="token-select-expiration"
                    className="spacer-top abs-width-100 spacer-right"
                    isSearchable={false}
                    onChange={this.handleTokenExpirationChange}
                    options={tokenExpirationOptions}
                    value={tokenExpirationOptions.find(
                      (option) => option.value === tokenExpiration
                    )}
                  />

                  {loading ? (
                    <i className="spinner text-middle" />
                  ) : (
                    <SubmitButton className="text-middle spacer-top" disabled={!tokenName}>
                      {translate('onboarding.token.generate')}
                    </SubmitButton>
                  )}
                </div>
              </div>
            </form>
            <ProjectTokenScopeInfo className="width-50" />
          </div>
        )}
      </div>
    );
  };

  renderUseExistingOption = () => {
    const { existingToken } = this.state;
    const validInput = !existingToken || existingToken.match(TOKEN_FORMAT_REGEX) != null;

    return (
      <div className="big-spacer-top">
        <Radio
          checked={this.state.selection === 'use-existing'}
          onCheck={this.handleModeChange}
          value="use-existing"
        >
          {translate('onboarding.token.use_existing_token')}
        </Radio>
        {this.state.selection === 'use-existing' && (
          <div className="big-spacer-top display-flex-column">
            <label className="h3" htmlFor="existing-token-input">
              {translate('onboarding.token.use_existing_token.label')}
              <DocumentationTooltip
                className="spacer-left"
                content={translate('onboarding.token.use_existing_token.help')}
                links={[
                  {
                    href: '/user-guide/user-account/generating-and-using-tokens/',
                    label: translate('learn_more'),
                  },
                ]}
              />
            </label>
            <input
              id="existing-token-input"
              autoFocus={true}
              className="input-super-large spacer-right spacer-top text-middle"
              onChange={this.handleExisingTokenChange}
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
              ),
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
