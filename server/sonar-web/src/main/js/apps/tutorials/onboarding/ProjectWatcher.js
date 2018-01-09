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
// @flow
import React from 'react';
import { getTasksForComponent } from '../../../api/ce';
import { STATUSES } from '../../../apps/background-tasks/constants';
import { translate } from '../../../helpers/l10n';

const INTERVAL = 5000;
const TIMEOUT = 10 * 60 * 1000; // 10 min

/*::
type Props = {
  onFinish: () => void,
  onTimeout: () => void,
  projectKey: string
};
*/

/*::
type State = {
  inQueue: boolean,
  status: ?string
};
*/

export default class ProjectWatcher extends React.PureComponent {
  /*:: interval: number; */
  /*:: mounted: boolean; */
  /*:: props: Props; */
  /*:: timeout: number; */
  state /*: State */ = {
    inQueue: false,
    status: null
  };

  componentDidMount() {
    this.mounted = true;
    this.watch();
    this.timeout = setTimeout(this.props.onTimeout, TIMEOUT);
  }

  componentWillUnmount() {
    clearInterval(this.interval);
    clearInterval(this.timeout);
    this.mounted = false;
  }

  watch = () => (this.interval = setTimeout(this.checkProject, INTERVAL));

  checkProject = () => {
    const { projectKey } = this.props;
    getTasksForComponent(projectKey).then(
      response => {
        if (response.queue.length > 0) {
          this.setState({ inQueue: true });
        }

        if (response.current != null) {
          const { status } = response.current;
          this.setState({ status });
          if (status === STATUSES.SUCCESS) {
            this.props.onFinish();
          } else if (status === STATUSES.PENDING || status === STATUSES.IN_PROGRESS) {
            this.watch();
          }
        } else {
          this.watch();
        }
      },
      () => {}
    );
  };

  render() {
    const { inQueue, status } = this.state;

    if (status === STATUSES.SUCCESS) {
      return (
        <div className="big-spacer-top note text-center">
          <i className="icon-check spacer-right" />
          {translate('onboarding.project_watcher.finished')}
        </div>
      );
    }

    if (inQueue || status === STATUSES.PENDING || status === STATUSES.IN_PROGRESS) {
      return (
        <div className="big-spacer-top note text-center">
          <i className="spinner spacer-right" />
          {translate('onboarding.project_watcher.in_progress')}
        </div>
      );
    }

    if (status != null) {
      return (
        <div className="big-spacer-top note text-center">
          <i className="icon-alert-danger spacer-right" />
          {translate('onboarding.project_watcher.failed')}
        </div>
      );
    }

    return (
      <div className="big-spacer-top note text-center">
        {translate('onboarding.project_watcher.not_started')}
      </div>
    );
  }
}
