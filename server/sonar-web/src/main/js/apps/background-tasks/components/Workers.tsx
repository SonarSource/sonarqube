/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import {
  FlagWarningIcon,
  HelperHintIcon,
  InteractiveIcon,
  PencilIcon,
  Spinner,
} from 'design-system';
import * as React from 'react';
import HelpTooltip from '~sonar-aligned/components/controls/HelpTooltip';
import { getWorkers } from '../../../api/ce';
import Tooltip from '../../../components/controls/Tooltip';
import { translate } from '../../../helpers/l10n';
import NoWorkersSupportPopup from './NoWorkersSupportPopup';
import WorkersForm from './WorkersForm';

interface State {
  canSetWorkerCount: boolean;
  formOpen: boolean;
  loading: boolean;
  workerCount: number;
}

export default class Workers extends React.PureComponent<{}, State> {
  mounted = false;
  state: State = {
    canSetWorkerCount: false,
    formOpen: false,
    loading: true,
    workerCount: 1,
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
            workerCount: value,
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      },
    );
  };

  closeForm = (newWorkerCount?: number) =>
    newWorkerCount
      ? this.setState({ formOpen: false, workerCount: newWorkerCount })
      : this.setState({ formOpen: false });

  handleChangeClick = () => {
    this.setState({ formOpen: true });
  };

  render() {
    const { canSetWorkerCount, formOpen, loading, workerCount } = this.state;

    return (
      <div className="sw-flex sw-items-center">
        {!loading && workerCount > 1 && (
          <Tooltip overlay={translate('background_tasks.number_of_workers.warning')}>
            <div className="sw-py-1/2 sw-mr-1">
              <FlagWarningIcon />
            </div>
          </Tooltip>
        )}

        <span id="ww">{translate('background_tasks.number_of_workers')}</span>

        <Spinner className="sw-ml-1" loading={loading}>
          <strong aria-labelledby="ww" className="sw-ml-1">
            {workerCount}
          </strong>
        </Spinner>

        {!loading && canSetWorkerCount && (
          <Tooltip overlay={translate('background_tasks.change_number_of_workers')}>
            <InteractiveIcon
              Icon={PencilIcon}
              aria-label={translate('background_tasks.change_number_of_workers')}
              className="js-edit sw-ml-2"
              onClick={this.handleChangeClick}
            />
          </Tooltip>
        )}

        {!loading && !canSetWorkerCount && (
          <HelpTooltip className="sw-ml-2" overlay={<NoWorkersSupportPopup />}>
            <HelperHintIcon />
          </HelpTooltip>
        )}

        {formOpen && <WorkersForm onClose={this.closeForm} workerCount={this.state.workerCount} />}
      </div>
    );
  }
}
