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
import { ReportStatus, subscribe, unsubscribe } from '../../../api/report';
import { translate, translateWithParameters } from '../../../helpers/l10n';

interface Props {
  component: string;
  currentUser: { email?: string };
  status: ReportStatus;
}

interface State {
  loading: boolean;
  subscribed?: boolean;
}

export default class Subscription extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = { subscribed: props.status.subscribed, loading: false };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.status.subscribed !== this.props.status.subscribed) {
      this.setState({ subscribed: nextProps.status.subscribed });
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  handleSubscription = (subscribed: boolean) => {
    if (this.mounted) {
      this.setState({ loading: false, subscribed });
    }
  };

  handleSubscribe = (e: React.SyntheticEvent<HTMLButtonElement>) => {
    e.preventDefault();
    e.currentTarget.blur();
    this.setState({ loading: true });
    subscribe(this.props.component)
      .then(() => this.handleSubscription(true))
      .catch(this.stopLoading);
  };

  handleUnsubscribe = (e: React.SyntheticEvent<HTMLButtonElement>) => {
    e.preventDefault();
    e.currentTarget.blur();
    this.setState({ loading: true });
    unsubscribe(this.props.component)
      .then(() => this.handleSubscription(false))
      .catch(this.stopLoading);
  };

  getEffectiveFrequencyText = () => {
    const effectiveFrequency =
      this.props.status.componentFrequency || this.props.status.globalFrequency;
    return translate('report.frequency', effectiveFrequency, 'effective');
  };

  renderLoading = () => this.state.loading && <i className="spacer-left spinner" />;

  renderWhenSubscribed = () => (
    <div className="js-subscribed">
      <div className="spacer-bottom">
        <i className="icon-check pull-left spacer-right" />
        <div className="overflow-hidden">
          {translateWithParameters('report.subscribed', this.getEffectiveFrequencyText())}
        </div>
      </div>
      <button onClick={this.handleUnsubscribe}>{translate('report.unsubscribe')}</button>
      {this.renderLoading()}
    </div>
  );

  renderWhenNotSubscribed = () => (
    <div className="js-not-subscribed">
      <p className="spacer-bottom">
        {translateWithParameters('report.unsubscribed', this.getEffectiveFrequencyText())}
      </p>
      <button className="js-report-subscribe" onClick={this.handleSubscribe}>
        {translate('report.subscribe')}
      </button>
      {this.renderLoading()}
    </div>
  );

  render() {
    const hasEmail = !!this.props.currentUser.email;
    const { subscribed } = this.state;

    let inner;
    if (hasEmail) {
      inner = subscribed ? this.renderWhenSubscribed() : this.renderWhenNotSubscribed();
    } else {
      inner = <p className="note js-no-email">{translate('report.no_email_to_subscribe')}</p>;
    }

    return <div className="big-spacer-top js-report-subscription">{inner}</div>;
  }
}
