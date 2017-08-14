/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import WorkersForm from './WorkersForm';
import Tooltip from '../../../components/controls/Tooltip';
import { getWorkers } from '../../../api/ce';
import { translate } from '../../../helpers/l10n';

/*::
type State = {
  canSetWorkerCount: boolean,
  formOpen: boolean,
  loading: boolean,
  workerCount: number
};
*/

export default class Workers extends React.PureComponent {
  /*:: mounted: boolean; */
  state /*: State */ = {
    canSetWorkerCount: false,
    formOpen: false,
    loading: true,
    workerCount: 1
  };

  componentDidMount() {
    this.mounted = true;
    this.loadWorkers();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadWorkers = () => {
    this.setState({ loading: true });
    getWorkers().then(({ canSetWorkerCount, value }) => {
      if (this.mounted) {
        this.setState({
          canSetWorkerCount,
          loading: false,
          workerCount: value
        });
      }
    });
  };

  closeForm = (newWorkerCount /*: ?number */) =>
    newWorkerCount
      ? this.setState({ formOpen: false, workerCount: newWorkerCount })
      : this.setState({ formOpen: false });

  handleChangeClick = (event /*: Event */) => {
    event.preventDefault();
    this.setState({ formOpen: true });
  };

  render() {
    const { canSetWorkerCount, formOpen, loading, workerCount } = this.state;

    return (
      <div>
        {!loading &&
          workerCount > 1 &&
          <Tooltip overlay={translate('background_tasks.number_of_workers.warning')}>
            <i className="icon-alert-warn little-spacer-right bt-workers-warning-icon" />
          </Tooltip>}

        {translate('background_tasks.number_of_workers')}

        {loading
          ? <i className="spinner little-spacer-left" />
          : <strong className="little-spacer-left">
              {workerCount}
            </strong>}

        {!loading &&
          (canSetWorkerCount
            ? <Tooltip overlay={translate('background_tasks.change_number_of_workers')}>
                <a className="icon-edit spacer-left" href="#" onClick={this.handleChangeClick} />
              </Tooltip>
            : <a
                className="button button-promote spacer-left"
                href="https://redirect.sonarsource.com/plugins/governance.html"
                target="_blank">
                {translate('background_tasks.add_more_with_governance')}
              </a>)}

        {formOpen && <WorkersForm onClose={this.closeForm} workerCount={this.state.workerCount} />}
      </div>
    );
  }
}
