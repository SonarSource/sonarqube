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
import { EditButton } from 'sonar-ui-common/components/controls/buttons';
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import AlertWarnIcon from 'sonar-ui-common/components/icons/AlertWarnIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getWorkers } from '../../../api/ce';
import NoWorkersSupportPopup from './NoWorkersSupportPopup';
import WorkersForm from './WorkersForm';

interface State {
  canSetWorkerCount: boolean;
  formOpen: boolean;
  loading: boolean;
  noSupportPopup: boolean;
  workerCount: number;
}

export default class Workers extends React.PureComponent<{}, State> {
  mounted = false;
  state: State = {
    canSetWorkerCount: false,
    formOpen: false,
    loading: true,
    noSupportPopup: false,
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
    getWorkers().then(
      ({ canSetWorkerCount, value }) => {
        if (this.mounted) {
          this.setState({
            canSetWorkerCount,
            loading: false,
            workerCount: value
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  closeForm = (newWorkerCount?: number) =>
    newWorkerCount
      ? this.setState({ formOpen: false, workerCount: newWorkerCount })
      : this.setState({ formOpen: false });

  handleChangeClick = () => {
    this.setState({ formOpen: true });
  };

  handleHelpClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    event.stopPropagation();
    this.toggleNoSupportPopup();
  };

  toggleNoSupportPopup = (show?: boolean) => {
    if (show !== undefined) {
      this.setState({ noSupportPopup: show });
    } else {
      this.setState(state => ({ noSupportPopup: !state.noSupportPopup }));
    }
  };

  render() {
    const { canSetWorkerCount, formOpen, loading, workerCount } = this.state;

    return (
      <div className="display-flex-center">
        {!loading && workerCount > 1 && (
          <Tooltip overlay={translate('background_tasks.number_of_workers.warning')}>
            <span className="display-inline-flex-center little-spacer-right">
              <AlertWarnIcon fill="#d3d3d3" />
            </span>
          </Tooltip>
        )}

        <span className="text-middle">
          {translate('background_tasks.number_of_workers')}

          {loading ? (
            <i className="spinner little-spacer-left" />
          ) : (
            <strong className="little-spacer-left">{workerCount}</strong>
          )}
        </span>

        {!loading && canSetWorkerCount && (
          <Tooltip overlay={translate('background_tasks.change_number_of_workers')}>
            <EditButton
              className="js-edit button-small spacer-left"
              onClick={this.handleChangeClick}
            />
          </Tooltip>
        )}

        {!loading && !canSetWorkerCount && (
          <HelpTooltip className="spacer-left" overlay={<NoWorkersSupportPopup />} />
        )}

        {formOpen && <WorkersForm onClose={this.closeForm} workerCount={this.state.workerCount} />}
      </div>
    );
  }
}
