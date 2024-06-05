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
import {
  BasicSeparator,
  ButtonPrimary,
  FlagMessage,
  FormField,
  InputField,
  InputTextArea,
  Spinner,
  SubHeading,
} from 'design-system';
import * as React from 'react';
import { sendTestEmail } from '../../../api/settings';
import withCurrentUserContext from '../../../app/components/current-user/withCurrentUserContext';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { parseError } from '../../../helpers/request';
import { LoggedInUser } from '../../../types/users';

interface Props {
  currentUser: LoggedInUser;
}

interface State {
  error?: string;
  loading: boolean;
  message: string;
  recipient: string;
  subject: string;
  success?: string;
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
      <>
        <BasicSeparator />
        <div className="sw-p-6 sw-flex sw-gap-12">
          <div className="sw-w-abs-300">
            <SubHeading>{translate('email_configuration.test.title')}</SubHeading>
            <div className="sw-mt-1">
              <MandatoryFieldsExplanation />
            </div>
          </div>

          <form className="sw-flex-1" onSubmit={this.handleFormSubmit}>
            {success && (
              <FlagMessage variant="success">
                {translateWithParameters('email_configuration.test.email_was_sent_to_x', success)}
              </FlagMessage>
            )}

            {error !== undefined && <FlagMessage variant="error">{error}</FlagMessage>}

            <FormField label={translate('email_configuration.test.to_address')} required>
              <InputField
                disabled={loading}
                id="test-email-to"
                onChange={this.onRecipientChange}
                required
                size="large"
                type="email"
                value={recipient}
              />
            </FormField>
            <FormField label={translate('email_configuration.test.subject')}>
              <InputField
                disabled={loading}
                id="test-email-subject"
                onChange={this.onSubjectChange}
                size="large"
                type="text"
                value={subject}
              />
            </FormField>
            <FormField label={translate('email_configuration.test.message')} required>
              <InputTextArea
                disabled={loading}
                id="test-email-message"
                onChange={this.onMessageChange}
                required
                rows={5}
                size="large"
                value={message}
              />
            </FormField>

            <ButtonPrimary disabled={loading} type="submit" className="sw-mt-2">
              {translate('email_configuration.test.send')}
            </ButtonPrimary>
            <Spinner loading={loading} className="sw-ml-2" />
          </form>
        </div>
      </>
    );
  }
}

export default withCurrentUserContext(EmailForm);
