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
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getValues } from '../../../api/settings';
import LongBranchesPatternForm from './LongBranchesPatternForm';

interface Props {
  project: string;
}

interface State {
  formOpen: boolean;
  setting?: T.SettingValue;
}

export const LONG_BRANCH_PATTERN = 'sonar.branch.longLivedBranches.regex';

export default class LongBranchesPattern extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { formOpen: false };

  componentDidMount() {
    this.mounted = true;
    this.fetchSetting();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchSetting() {
    return getValues({ keys: LONG_BRANCH_PATTERN, component: this.props.project }).then(
      settings => {
        if (this.mounted) {
          this.setState({ setting: settings[0] });
        }
      },
      () => {}
    );
  }

  closeForm = () => {
    if (this.mounted) {
      this.setState({ formOpen: false });
    }
  };

  handleChangeClick = () => {
    this.setState({ formOpen: true });
  };

  handleChange = () => {
    if (this.mounted) {
      this.fetchSetting().then(this.closeForm, this.closeForm);
    }
  };

  render() {
    const { setting } = this.state;

    if (!setting) {
      return null;
    }

    return (
      <div className="pull-right text-right">
        {translate('branches.long_living_branches_pattern')}
        {': '}
        <strong>{setting.value}</strong>
        <EditButton className="button-small spacer-left" onClick={this.handleChangeClick} />
        {this.state.formOpen && (
          <LongBranchesPatternForm
            onChange={this.handleChange}
            onClose={this.closeForm}
            project={this.props.project}
            setting={setting}
          />
        )}
      </div>
    );
  }
}
