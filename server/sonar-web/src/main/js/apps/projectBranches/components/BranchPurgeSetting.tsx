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
import * as React from 'react';
import { excludeBranchFromPurge } from '../../../api/branches';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import Toggle from '../../../components/controls/Toggle';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { isMainBranch } from '../../../helpers/branch-like';
import { translate } from '../../../helpers/l10n';
import { Branch } from '../../../types/branch-like';
import { Component } from '../../../types/types';

interface Props {
  branch: Branch;
  component: Component;
  onUpdatePurgeSetting: () => void;
}

interface State {
  excludedFromPurge: boolean;
  loading: boolean;
}

export default class BranchPurgeSetting extends React.PureComponent<Props, State> {
  mounted = false;

  constructor(props: Props) {
    super(props);

    this.state = { excludedFromPurge: props.branch.excludedFromPurge, loading: false };
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleOnChange = () => {
    const { branch, component } = this.props;
    const { excludedFromPurge } = this.state;
    const newValue = !excludedFromPurge;

    this.setState({ loading: true });

    excludeBranchFromPurge(component.key, branch.name, newValue)
      .then(() => {
        if (this.mounted) {
          this.setState({
            excludedFromPurge: newValue,
            loading: false,
          });
          this.props.onUpdatePurgeSetting();
        }
      })
      .catch(() => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      });
  };

  render() {
    const { branch } = this.props;
    const { excludedFromPurge, loading } = this.state;

    const isTheMainBranch = isMainBranch(branch);
    const disabled = isTheMainBranch || loading;

    return (
      <>
        <Toggle disabled={disabled} onChange={this.handleOnChange} value={excludedFromPurge} />
        <span className="spacer-left">
          <DeferredSpinner loading={loading} />
        </span>
        {isTheMainBranch && (
          <HelpTooltip
            overlay={translate(
              'project_branch_pull_request.branch.auto_deletion.main_branch_tooltip'
            )}
          />
        )}
      </>
    );
  }
}
