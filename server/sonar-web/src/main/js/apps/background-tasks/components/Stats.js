/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
 /* @flow */
import React from 'react';
import shallowCompare from 'react-addons-shallow-compare';

import { translate } from '../../../helpers/l10n';

export default class Stats extends React.Component {
  static propTypes = {
    failingCount: React.PropTypes.number,
    pendingCount: React.PropTypes.number,
    onShowFailing: React.PropTypes.func.isRequired,
    onCancelAllPending: React.PropTypes.func.isRequired
  };

  shouldComponentUpdate (nextProps: any, nextState: any) {
    return shallowCompare(this, nextProps, nextState);
  }

  handleCancelAllPending (e: any) {
    e.preventDefault();
    e.target.blur();
    this.props.onCancelAllPending();
  }

  handleShowFailing (e: any) {
    e.preventDefault();
    e.target.blur();
    this.props.onShowFailing();
  }

  renderPending () {
    if (this.props.pendingCount == null) {
      return null;
    }
    if (this.props.pendingCount > 0) {
      return (
          <span>
            <span className="js-pending-count emphasised-measure">{this.props.pendingCount}</span>
            &nbsp;
            {translate('background_tasks.pending')}
            <a
                onClick={this.handleCancelAllPending.bind(this)}
                className="js-cancel-pending icon-delete spacer-left"
                title={translate('background_tasks.cancel_all_tasks')}
                data-toggle="tooltip"
                href="#"/>
          </span>
      );
    } else {
      return (
          <span>
            <span className="js-pending-count emphasised-measure">{this.props.pendingCount}</span>
            &nbsp;
            {translate('background_tasks.pending')}
          </span>
      );
    }
  }

  renderFailures () {
    if (this.props.failingCount == null) {
      return null;
    }

    if (this.props.component) {
      return null;
    }

    if (this.props.failingCount > 0) {
      return (
          <span>
            <a onClick={this.handleShowFailing.bind(this)}
               className="js-failures-count emphasised-measure"
               data-toggle="tooltip"
               title="Count of projects where processing of most recent analysis report failed"
               href="#">{this.props.failingCount}</a>
            &nbsp;
            {translate('background_tasks.failures')}
          </span>
      );
    } else {
      return (
          <span>
            <span className="js-failures-count emphasised-measure" data-toggle="tooltip"
                  title="Count of projects where processing of most recent analysis report failed">
              {this.props.failingCount}
            </span>
            &nbsp;
            {translate('background_tasks.failures')}
          </span>
      );
    }
  }

  render () {
    return (
        <section className="big-spacer-top big-spacer-bottom">
          <span>
            {this.renderPending()}
          </span>
          <span className="huge-spacer-left">
            {this.renderFailures()}
          </span>
        </section>

    );
  }
}
