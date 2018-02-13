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
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import BranchRow from './BranchRow';
import LongBranchesPattern from './LongBranchesPattern';
import { Branch } from '../../../app/types';
import { sortBranchesAsTree } from '../../../helpers/branches';
import { translate } from '../../../helpers/l10n';
import { getValues } from '../../../api/settings';
import { formatMeasure } from '../../../helpers/measures';

interface Props {
  branches: Branch[];
  canAdmin?: boolean;
  component: { key: string };
  onBranchesChange: () => void;
}

interface State {
  branchLifeTime?: string;
  loading: boolean;
}

const BRANCH_LIFETIME_SETTING = 'sonar.dbcleaner.daysBeforeDeletingInactiveShortLivingBranches';

export default class App extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    this.fetchPurgeSetting();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchPurgeSetting() {
    this.setState({ loading: true });
    getValues(BRANCH_LIFETIME_SETTING).then(
      settings => {
        if (this.mounted) {
          this.setState({
            loading: false,
            branchLifeTime: settings.length > 0 ? settings[0].value : undefined
          });
        }
      },
      () => {
        this.setState({ loading: false });
      }
    );
  }

  renderBranchLifeTime() {
    const { branchLifeTime } = this.state;
    if (!branchLifeTime) {
      return null;
    }

    const messageKey = this.props.canAdmin
      ? 'project_branches.page.life_time.admin'
      : 'project_branches.page.life_time';

    return (
      <p className="page-description">
        <FormattedMessage
          defaultMessage={translate(messageKey)}
          id={messageKey}
          values={{
            days: formatMeasure(this.state.branchLifeTime, 'INT'),
            settings: <Link to="/admin/settings">{translate('settings.page')}</Link>
          }}
        />
      </p>
    );
  }

  render() {
    const { branches, component, onBranchesChange } = this.props;

    if (this.state.loading) {
      return (
        <div className="page page-limited">
          <header className="page-header">
            <h1 className="page-title">{translate('project_branches.page')}</h1>
          </header>
          <i className="spinner" />
        </div>
      );
    }

    return (
      <div className="page page-limited">
        <header className="page-header">
          <h1 className="page-title">{translate('project_branches.page')}</h1>
          <LongBranchesPattern project={component.key} />
          <p className="page-description">{translate('project_branches.page.description')}</p>
          {this.renderBranchLifeTime()}
        </header>

        <div className="boxed-group boxed-group-inner">
          <table className="data zebra zebra-hover">
            <thead>
              <tr>
                <th>{translate('branch')}</th>
                <th className="thin nowrap text-right">{translate('status')}</th>
                <th className="thin nowrap text-right">
                  {translate('branches.last_analysis_date')}
                </th>
                <th className="thin nowrap text-right">{translate('actions')}</th>
              </tr>
            </thead>
            <tbody>
              {sortBranchesAsTree(branches).map(branch => (
                <BranchRow
                  branch={branch}
                  component={component.key}
                  key={branch.name}
                  onChange={onBranchesChange}
                />
              ))}
            </tbody>
          </table>
        </div>
      </div>
    );
  }
}
