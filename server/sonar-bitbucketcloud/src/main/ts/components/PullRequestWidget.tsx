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
import Spinner from '@atlaskit/spinner';
import QuestionCircleIcon from '@atlaskit/icon/glyph/question-circle';
import BugIcon from '@sqcore/components/icons-components/BugIcon';
import CodeSmellIcon from '@sqcore/components/icons-components/CodeSmellIcon';
import StatusIndicator from '@sqcore/components/common/StatusIndicator';
import VulnerabilityIcon from '@sqcore/components/icons-components/VulnerabilityIcon';
import { formatMeasure } from '@sqcore/helpers/measures';
import {
  getPullRequestUrl,
  getPathUrlAsString,
  getShortLivingBranchUrl
} from '@sqcore/helpers/urls';
import { getBranchQualityGateColor } from '@sqcore/helpers/branches';
import { getPullRequestData } from '../api';
import { PullRequestData, PullRequestContext } from '../types';
import { getRepoSettingsUrl, isRepoAdmin } from '../utils';

interface Props {
  context: PullRequestContext;
}

interface State {
  error?: string;
  loading: boolean;
  pullRequest?: PullRequestData;
  settingsUrl?: string;
}

export default class PullRequestWidget extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    this.fetchPullRequestData();
    getRepoSettingsUrl().then(
      settingsUrl => {
        if (this.mounted) {
          this.setState({ settingsUrl });
        }
      },
      () => {}
    );
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchPullRequestData = () => {
    getPullRequestData(this.props.context).then(
      pullRequest => {
        if (this.mounted) {
          this.setState({ error: undefined, loading: false, pullRequest });
        }
      },
      ({ errors }) => {
        if (this.mounted) {
          this.setState({ error: errors && errors[0] && errors[0].msg, loading: false });
        }
      }
    );
  };

  renderStatus = (pullRequest: PullRequestData) => {
    return (
      <span title={`Quality Gate ${formatMeasure(pullRequest.status.qualityGateStatus, 'LEVEL')}`}>
        <StatusIndicator
          className="little-spacer-left little-spacer-right"
          color={getBranchQualityGateColor(pullRequest.status.qualityGateStatus)}
          size="small"
        />
      </span>
    );
  };

  render() {
    const { error, loading, pullRequest, settingsUrl } = this.state;

    if (loading) {
      return (
        <div className="pr-widget">
          <Spinner size="small" />
        </div>
      );
    }

    if (!pullRequest) {
      if (error && error.includes('Repository not bound')) {
        const settingsLink = settingsUrl ? (
          <a href={settingsUrl} rel="noopener noreferrer" target="_parent">
            repository settings
          </a>
        ) : (
          'repository settings'
        );
        return (
          <p className="pr-widget empty">
            {isRepoAdmin() ? (
              <>No link between repository and SonarCloud project. Go to your {settingsLink}.</>
            ) : (
              'No link between repository and SonarCloud project. Contact an administrator.'
            )}
          </p>
        );
      }
      return <p className="pr-widget empty">Not analyzed on SonarCloud yet</p>;
    }

    const totalIssues =
      pullRequest.status.bugs + pullRequest.status.vulnerabilities + pullRequest.status.codeSmells;

    if (totalIssues <= 0) {
      return (
        <p className="pr-widget">
          {this.renderStatus(pullRequest)}
          {"Hooray! SonarCloud analysis didn't find any issue!"}
        </p>
      );
    }

    const shouldDisplayHelper = pullRequest.status.qualityGateStatus === 'OK';

    const prUrl = getPathUrlAsString(
      pullRequest.isPullRequest !== undefined
        ? getPullRequestUrl(pullRequest.projectKey, pullRequest.key)
        : getShortLivingBranchUrl(pullRequest.projectKey, pullRequest.key)
    );

    return (
      <div className="pr-widget">
        <div className="pr-widget-inner">
          {this.renderStatus(pullRequest)}
          <span className="little-spacer-right">SonarCloud found issues:</span>
          <span className="little-spacer-right">
            <BugIcon className="little-spacer-right" />{' '}
            {formatMeasure(pullRequest.status.bugs, 'SHORT_INT')} Bugs
          </span>
          <span className="little-spacer-right">
            <VulnerabilityIcon className="little-spacer-right" />{' '}
            {formatMeasure(pullRequest.status.vulnerabilities, 'SHORT_INT')} Vulnerabilities
          </span>
          <span className="little-spacer-right">
            <CodeSmellIcon className="little-spacer-right" />{' '}
            {formatMeasure(pullRequest.status.codeSmells, 'SHORT_INT')} Code Smells
          </span>
          {shouldDisplayHelper && (
            <span
              title={`The branch Quality Gate is "Passed" because there are no open issue. The remaining ${totalIssues} issue(s) have been confirmed.`}>
              <QuestionCircleIcon label="help" primaryColor="rgb(180, 180, 180)" size="small" />
            </span>
          )}
        </div>
        <a href={prUrl} target="_blank">
          See all issues on SonarCloud
        </a>
      </div>
    );
  }
}
