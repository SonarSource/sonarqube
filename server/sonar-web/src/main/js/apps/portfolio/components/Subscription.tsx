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
import { connect } from 'react-redux';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { ReportStatus, subscribe, unsubscribe } from '../../../api/report';
import addGlobalSuccessMessage from '../../../app/utils/addGlobalSuccessMessage';
import throwGlobalError from '../../../app/utils/throwGlobalError';
import { isLoggedIn } from '../../../helpers/users';
import { getCurrentUser, Store } from '../../../store/rootReducer';

interface Props {
  component: string;
  currentUser: T.CurrentUser;
  onSubscribe: () => void;
  status: ReportStatus;
}

export class Subscription extends React.PureComponent<Props> {
  handleSubscription = (subscribed: boolean) => {
    addGlobalSuccessMessage(
      subscribed
        ? translateWithParameters('report.subscribe_x_success', this.getFrequencyText())
        : translateWithParameters('report.unsubscribe_x_success', this.getFrequencyText())
    );
    this.props.onSubscribe();
  };

  handleSubscribe = () => {
    subscribe(this.props.component)
      .then(() => this.handleSubscription(true))
      .catch(throwGlobalError);
  };

  handleUnsubscribe = () => {
    unsubscribe(this.props.component)
      .then(() => this.handleSubscription(false))
      .catch(throwGlobalError);
  };

  getFrequencyText = () => {
    const effectiveFrequency =
      this.props.status.componentFrequency || this.props.status.globalFrequency;
    return translate('report.frequency', effectiveFrequency);
  };

  render() {
    const hasEmail = isLoggedIn(this.props.currentUser) && !!this.props.currentUser.email;

    const { status } = this.props;

    if (!hasEmail) {
      return <span className="text-muted-2">{translate('report.no_email_to_subscribe')}</span>;
    }

    return status.subscribed ? (
      <a href="#" onClick={this.handleUnsubscribe}>
        {translateWithParameters('report.unsubscribe_x', this.getFrequencyText())}
      </a>
    ) : (
      <a href="#" onClick={this.handleSubscribe}>
        {translateWithParameters('report.subscribe_x', this.getFrequencyText())}
      </a>
    );
  }
}

const mapStateToProps = (state: Store) => ({
  currentUser: getCurrentUser(state)
});

export default connect(mapStateToProps)(Subscription);
