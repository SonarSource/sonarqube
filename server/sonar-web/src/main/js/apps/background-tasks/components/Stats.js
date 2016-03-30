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
import React, { Component } from 'react';

import { translate } from '../../../helpers/l10n';

export default class Stats extends Component {
  handleCancelAllPending (e) {
    e.preventDefault();
    this.props.onCancelAllPending();
  }

  handleShowFailing (e) {
    e.preventDefault();
    this.props.onShowFailing();
  }

  renderPending () {
    if (this.props.pendingCount == null) {
      return null;
    }
    if (this.props.pendingCount > 0) {
      return (
          <span>
            <span ref="pendingCount" className="emphasised-measure">{this.props.pendingCount}</span>
            &nbsp;
            {translate('background_tasks.pending')}
            <a
                ref="cancelPending"
                onClick={this.handleCancelAllPending.bind(this)}
                className="icon-delete spacer-left"
                title={translate('background_tasks.cancel_all_tasks')}
                data-toggle="tooltip"
                href="#"/>
          </span>
      );
    } else {
      return (
          <span>
            <span ref="pendingCount" className="emphasised-measure">{this.props.pendingCount}</span>
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
            <a ref="failureCount"
               onClick={this.handleShowFailing.bind(this)}
               className="emphasised-measure"
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
            <span ref="failureCount" className="emphasised-measure" data-toggle="tooltip"
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
