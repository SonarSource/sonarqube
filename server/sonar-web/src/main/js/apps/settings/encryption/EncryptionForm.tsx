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
import ClipboardButton from '../../../components/controls/ClipboardButton';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import { SubmitButton } from '../../../components/ui/buttons';
import { encryptValue } from '../../../api/settings';
import { translate } from '../../../helpers/l10n';

interface Props {
  generateSecretKey: () => Promise<void>;
}

interface State {
  encryptedValue?: string;
  encrypting: boolean;
  generating: boolean;
  value: string;
}

export default class EncryptionForm extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { encrypting: false, generating: false, value: '' };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
    this.setState({ value: event.currentTarget.value });
  };

  handleEncrypt = (event: React.FormEvent) => {
    event.preventDefault();
    this.setState({ encrypting: true });
    encryptValue(this.state.value).then(
      ({ encryptedValue }) => {
        if (this.mounted) {
          this.setState({ encryptedValue, encrypting: false });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ encrypting: false });
        }
      }
    );
  };

  handleGenerateSecretKey = (event: React.FormEvent) => {
    event.preventDefault();
    this.setState({ generating: true });
    this.props.generateSecretKey().then(this.stopGenerating, this.stopGenerating);
  };

  stopGenerating = () => {
    if (this.mounted) {
      this.setState({ generating: false });
    }
  };

  render() {
    const { encryptedValue, encrypting, generating } = this.state;
    return (
      <div id="encryption-form-container">
        <div className="spacer-bottom">{translate('encryption.form_intro')}</div>
        <form className="big-spacer-bottom" id="encryption-form" onSubmit={this.handleEncrypt}>
          <textarea
            autoFocus={true}
            className="abs-width-600"
            id="encryption-form-value"
            onChange={this.handleChange}
            required={true}
            rows={5}
            value={this.state.value}
          />
          <div className="spacer-top">
            <SubmitButton disabled={encrypting || generating}>
              {translate('encryption.encrypt')}
            </SubmitButton>
            <DeferredSpinner className="spacer-left" loading={encrypting} />
          </div>
        </form>

        {encryptedValue && (
          <div>
            <span className="little-spacer-right">{translate('encryption.encrypted_value')}</span>
            <input
              className="input-clear input-code input-super-large"
              id="encrypted-value"
              readOnly={true}
              type="text"
              value={encryptedValue}
            />
            <ClipboardButton className="little-spacer-left" copyValue={encryptedValue} />
          </div>
        )}

        <form
          className="huge-spacer-top bordered-top"
          id="encryption-new-key-form"
          onSubmit={this.handleGenerateSecretKey}>
          <p className="big-spacer-top spacer-bottom">
            <FormattedMessage
              defaultMessage={translate('encryption.form_note')}
              id="encryption.form_note"
              values={{
                moreInformationLink: (
                  <a
                    href="https://redirect.sonarsource.com/doc/settings-encryption.html"
                    rel="noopener noreferrer"
                    target="_blank">
                    {translate('more_information')}
                  </a>
                )
              }}
            />
          </p>

          <SubmitButton disabled={generating || encrypting}>
            {translate('encryption.generate_new_secret_key')}{' '}
          </SubmitButton>
          <DeferredSpinner className="spacer-left" loading={generating} />
        </form>
      </div>
    );
  }
}
