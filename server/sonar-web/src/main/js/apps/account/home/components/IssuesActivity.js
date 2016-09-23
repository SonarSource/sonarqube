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
import React from 'react';
import { connect } from 'react-redux';
import { fetchIssuesActivity } from '../store/actions';
import { getIssuesActivity } from '../../../../app/store/rootReducer';
import { getIssuesUrl } from '../../../../helpers/urls';
import { translate } from '../../../../helpers/l10n';

class IssuesActivity extends React.Component {
  componentDidMount () {
    this.props.fetchIssuesActivity();
  }

  getColorClass (number) {
    if (number == null) {
      return '';
    }
    return number > 0 ? 'text-danger' : 'text-success';
  }

  renderRecentIssues () {
    const number = this.props.issuesActivity && this.props.issuesActivity.recent;
    const url = getIssuesUrl({ resolved: 'false', assignees: '__me__', createdInLast: '1w' });

    return (
        <a className="my-activity-recent-issues" href={url}>
          <div id="recent-issues" className={'my-activity-issues-number ' + this.getColorClass(number)}>
            {number != null ? number : ' ' }
          </div>
          <div className="my-activity-issues-note">
            {translate('my_activity.my_issues')}<br/>{translate('my_activity.last_week')}
          </div>
        </a>
    );
  }

  renderAllIssues () {
    const number = this.props.issuesActivity && this.props.issuesActivity.all;
    const url = getIssuesUrl({ resolved: 'false', assignees: '__me__' });

    return (
        <a className="my-activity-all-issues" href={url}>
          <div id="all-issues" className={'my-activity-issues-number ' + this.getColorClass(number)}>
            {number != null ? number : ' ' }
          </div>
          <div className="my-activity-issues-note">
            {translate('my_activity.my_issues')}<br/>{translate('my_activity.all_time')}
          </div>
        </a>
    );
  }

  render () {
    return (
        <div className="my-activity-issues">
          {this.renderRecentIssues()}
          {this.renderAllIssues()}
        </div>
    );
  }
}

export default connect(
    state => ({ issuesActivity: getIssuesActivity(state) }),
    { fetchIssuesActivity }
)(IssuesActivity);
