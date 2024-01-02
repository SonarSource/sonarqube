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
import { sendTestEmail } from '../../../api/settings';
import withCurrentUserContext from '../../../app/components/current-user/withCurrentUserContext';
import { SubmitButton } from '../../../components/controls/buttons';
import { Alert } from '../../../components/ui/Alert';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import MandatoryFieldMarker from '../../../components/ui/MandatoryFieldMarker';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { parseError } from '../../../helpers/request';
import { LoggedInUser } from '../../../types/users';

interface Props {
  currentUser: LoggedInUser;
}

interface State {
  recipient: string;
  subject: string;
  message: string;
  loading: boolean;
  success?: string;
  error?: string;
}

export class EmailForm extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      recipient: this.props.currentUser.email || '',
      subject: translate('email_configuration.test.subject'),
      message: translate('email_configuration.test.message_text'),
      loading: false,
    };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleError = (response: Response) => {
    return parseError(response).then((message) => {
      if (this.mounted) {
        this.setState({ error: message, loading: false });
      }
    });
  };

  handleFormSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    this.setState({ success: undefined, error: undefined, loading: true });
    const { recipient, subject, message } = this.state;
    sendTestEmail(recipient, subject, message).then(() => {
      if (this.mounted) {
        this.setState({ success: recipient, loading: false });
      }
    }, this.handleError);
  };

  onRecipientChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ recipient: event.target.value });
  };

  onSubjectChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ subject: event.target.value });
  };

  onMessageChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
    this.setState({ message: event.target.value });
  };

  render() {
    const { error, loading, message, recipient, subject, success } = this.state;
    return (
      <div className="settings-definition">
        <div className="settings-definition-left">
          <h3 className="settings-definition-name">
            {translate('email_configuration.test.title')}
          </h3>
        </div>

        <form className="settings-definition-right" onSubmit={this.handleFormSubmit}>
          {success && (
            <div className="form-field">
              <Alert variant="success">
                {translateWithParameters('email_configuration.test.email_was_sent_to_x', success)}
              </Alert>
            </div>
          )}

          {error !== undefined && (
            <div className="form-field">
              <Alert variant="error">{error}</Alert>
            </div>
          )}

          <MandatoryFieldsExplanation className="form-field" />

          <div className="form-field">
            <label htmlFor="test-email-to">
              {translate('email_configuration.test.to_address')}
              <MandatoryFieldMarker />
            </label>
            <input
              className="settings-large-input"
              disabled={loading}
              id="test-email-to"
              onChange={this.onRecipientChange}
              required={true}
              type="email"
              value={recipient}
            />
          </div>
          <div className="form-field">
            <label htmlFor="test-email-subject">
              {translate('email_configuration.test.subject')}
            </label>
            <input
              className="settings-large-input"
              disabled={loading}
              id="test-email-subject"
              onChange={this.onSubjectChange}
              type="text"
              value={subject}
            />
          </div>
          <div className="form-field">
            <label htmlFor="test-email-message">
              {translate('email_configuration.test.message')}
              <MandatoryFieldMarker />
            </label>
            <textarea
              className="settings-large-input"
              disabled={loading}
              id="test-email-message"
              onChange={this.onMessageChange}
              required={true}
              rows={5}
              value={message}
            />
          </div>

          <SubmitButton disabled={loading}>
            {translate('email_configuration.test.send')}
          </SubmitButton>
          {loading && <DeferredSpinner className="spacer-left" />}
        </form>
      </div>
    );
  }
}

export default withCurrentUserContext(EmailForm);
