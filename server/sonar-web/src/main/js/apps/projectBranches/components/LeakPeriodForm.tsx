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
import SettingForm from './SettingForm';
import { translate } from '../../../helpers/l10n';
import { getValues } from '../../../api/settings';
import Modal from '../../../components/controls/Modal';

interface Props {
  branch: string;
  onClose: () => void;
  project: string;
}

interface State {
  loading: boolean;
  setting?: T.SettingValue;
  submitting: boolean;
  value?: string;
}

const LEAK_PERIOD = 'sonar.leak.period';

export default class LeakPeriodForm extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true, submitting: false };

  componentDidMount() {
    this.mounted = true;
    this.fetchSetting();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchSetting() {
    this.setState({ loading: true });
    getValues({ keys: LEAK_PERIOD, component: this.props.project, branch: this.props.branch }).then(
      settings => {
        if (this.mounted) {
          this.setState({ loading: false, setting: settings[0] });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  }

  handleCancelClick = (event: React.SyntheticEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    this.props.onClose();
  };

  render() {
    const { setting } = this.state;
    const header = translate('branches.set_new_code_period');

    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>
        {this.state.loading && (
          <div className="modal-body">
            <i className="spinner" />
          </div>
        )}
        {setting && (
          <SettingForm
            branch={this.props.branch}
            onChange={this.props.onClose}
            onClose={this.props.onClose}
            project={this.props.project}
            setting={setting}
          />
        )}
      </Modal>
    );
  }
}
