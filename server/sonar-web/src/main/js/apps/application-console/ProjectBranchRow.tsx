/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { orderBy } from 'lodash';
import * as React from 'react';
import Checkbox from 'sonar-ui-common/components/controls/Checkbox';
import Select from 'sonar-ui-common/components/controls/Select';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import QualifierIcon from 'sonar-ui-common/components/icons/QualifierIcon';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { getBranches } from '../../api/branches';
import { ApplicationProject } from '../../types/application';
import BranchSelectItem from './BranchSelectItem';
import { ApplicationBranch, SelectBranchOption } from './utils';

interface Props {
  checked: boolean;
  onChange: (projectKey: string, branch: SelectBranchOption) => void;
  onCheck: (checked: boolean, id?: string) => void;
  onClose: () => void;
  onOpen: (selectNode: HTMLElement, elementCount: number) => void;
  project: ApplicationProject;
}

interface State {
  branches?: SelectBranchOption[];
  loading: boolean;
  selectedBranch?: SelectBranchOption;
}

export default class ProjectBranchRow extends React.PureComponent<Props, State> {
  node?: HTMLElement | null = null;
  mounted = false;
  state: State = { loading: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  parseBranches = (branches: Array<ApplicationBranch>) => {
    return orderBy(branches, [b => b.isMain, b => b.name]).map(branch => {
      return { value: branch.name, label: branch.name, isMain: branch.isMain };
    });
  };

  setCurrentTarget = (event: React.FocusEvent<HTMLInputElement>) => {
    this.node = event.target;
  };

  handleChange = (value: SelectBranchOption) => {
    this.props.onChange(this.props.project.key, value);
    this.setState({ selectedBranch: value });
  };

  handleOpen = () => {
    if (this.state.branches && this.node) {
      this.props.onOpen(this.node, this.state.branches.length);
      return;
    }

    const { project } = this.props;
    this.setState({ loading: true });
    getBranches(project.key).then(
      branchesResult => {
        const branches = this.parseBranches(branchesResult);
        if (this.node) {
          this.props.onOpen(this.node, branches.length);
        }
        if (this.mounted) {
          this.setState({ branches, loading: false });
        }
      },
      () => {
        /* Fail silently*/
      }
    );
  };

  render() {
    const { checked, onCheck, onClose, project } = this.props;
    const options = this.state.branches || [
      { value: project.branch, label: project.branch, isMain: project.isMain }
    ];
    const value = project.enabled
      ? this.state.selectedBranch || project.branch
      : this.state.selectedBranch;
    return (
      <tr key={project.key}>
        <td className="text-center">
          <Checkbox checked={checked} id={project.key} onCheck={onCheck} />
        </td>
        <td className="nowrap hide-overflow branch-name-row">
          <Tooltip overlay={project.name}>
            <span>
              <QualifierIcon qualifier="TRK" /> {project.name}
            </span>
          </Tooltip>
        </td>
        <td>
          <Select
            className="width100"
            clearable={false}
            disabled={!checked}
            onChange={this.handleChange}
            onClose={onClose}
            onFocus={this.setCurrentTarget}
            onOpen={this.handleOpen}
            optionComponent={BranchSelectItem}
            options={options}
            searchable={false}
            value={value}
          />
          <DeferredSpinner className="project-branch-row-spinner" loading={this.state.loading} />
        </td>
      </tr>
    );
  }
}
