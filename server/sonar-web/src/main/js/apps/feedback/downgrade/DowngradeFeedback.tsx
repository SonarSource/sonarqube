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
import { WithRouterProps } from 'react-router';
import { Alert } from '../../../components/ui/Alert';
import { SubmitButton } from '../../../components/ui/buttons';
import { translate } from '../../../helpers/l10n';
import { giveDowngradeFeedback } from '../../../api/billing';
import { getBaseUrl } from '../../../helpers/urls';
import addGlobalSuccessMessage from '../../../app/utils/addGlobalSuccessMessage';
import Radio from '../../../components/controls/Radio';
import './DowngradeFeedback.css';

enum Reason {
  doesntWork = 'did_not_work',
  doesntMeetExpectations = 'did_not_match_expectations',
  doesntMeetCompanyPolicy = 'company_policy',
  onlyTesting = 'just_testing',
  other = 'other'
}

interface State {
  additionalFeedback: string;
  feedback?: Reason;
}

export interface LocationState {
  confirmationMessage?: string;
  organization?: T.Organization;
  returnTo?: string;
  title: string;
}

export default class DowngradeFeedback extends React.PureComponent<WithRouterProps, State> {
  state: State = {
    additionalFeedback: ''
  };

  handleChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
    this.setState({
      additionalFeedback: event.currentTarget.value
    });
  };

  handleChoice = (value: string) => {
    this.setState({
      feedback: value as Reason
    });
  };

  handleSkip = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.navigateAway();
  };

  handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();

    const { organization } = this.getLocationState();
    if (!organization) {
      return;
    }

    giveDowngradeFeedback({
      organization: organization.key,
      feedback: this.state.feedback as string,
      additionalFeedback: this.state.additionalFeedback
    });

    this.navigateAway(translate('billing.downgrade.thankyou_for_feedback'));
  };

  getLocationState = (): LocationState => {
    const { location } = this.props;
    return location.state || {};
  };

  navigateAway = (message?: string) => {
    if (message) {
      addGlobalSuccessMessage(message);
    }

    const { returnTo } = this.getLocationState();
    this.props.router.replace({
      pathname: returnTo || getBaseUrl()
    });
  };

  render() {
    const { organization, confirmationMessage, title } = this.getLocationState();
    if (!organization) {
      return null;
    }

    return (
      <div className="billing-downgrade-feedback">
        {confirmationMessage && <Alert variant="success">{confirmationMessage}</Alert>}
        <div className="boxed-group boxed-group-centered">
          <div className="boxed-group-header">
            <h2>{title}</h2>
            <div>{translate('billing.downgrade.reason.explanation')}</div>
          </div>
          <div className="boxed-group-inner">
            <form className="billing-downgrade-feedback-form" onSubmit={this.handleSubmit}>
              <ul className="boxed-group-list">
                {Object.keys(Reason).map(key => {
                  const reason = Reason[key as any];
                  return (
                    <li key={reason}>
                      <Radio
                        checked={this.state.feedback === reason}
                        onCheck={this.handleChoice}
                        value={reason}>
                        {translate('billing.downgrade.reason.option', reason, 'label')}
                      </Radio>
                      {this.state.feedback === reason && (
                        <div className="billing-downgrade-feedback-explain-wrapper">
                          <label htmlFor={`reason_text_${reason}`}>
                            {translate('billing.why')}
                            <span className="note">{translate('billing.optional')}</span>
                          </label>
                          <textarea
                            id={`reason_text_${reason}`}
                            name={`reason_text_${reason}`}
                            onChange={this.handleChange}
                            placeholder={translate(
                              'billing.downgrade.reason.option',
                              reason,
                              'placeholder'
                            )}
                            value={this.state.additionalFeedback}
                          />
                        </div>
                      )}
                    </li>
                  );
                })}
              </ul>
              <div className="billing-downgrade-feedback-form-actions">
                <SubmitButton className="spacer-right" disabled={!this.state.feedback}>
                  {translate('billing.send')}
                </SubmitButton>
                <a href="#" onClick={this.handleSkip}>
                  {translate('billing.skip')}
                </a>
              </div>
            </form>
          </div>
        </div>
      </div>
    );
  }
}
