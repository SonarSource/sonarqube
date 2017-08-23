/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import * as PropTypes from 'prop-types';
import ComponentNavBranchesMenuItem from './ComponentNavBranchesMenuItem';
import { Branch, Component } from '../../../types';
import { getBranches } from '../../../../api/branches';
import {
  sortBranchesAsTree,
  isLongLivingBranch,
  isShortLivingBranch
} from '../../../../helpers/branches';
import { translate } from '../../../../helpers/l10n';
import { getProjectBranchUrl } from '../../../../helpers/urls';

interface Props {
  branch: Branch;
  onClose: () => void;
  project: Component;
}

interface State {
  branches: Branch[];
  loading: boolean;
  query: string;
  selected: string | null;
}

export default class ComponentNavBranchesMenu extends React.PureComponent<Props, State> {
  private mounted: boolean;
  private node: HTMLElement | null;

  static contextTypes = {
    router: PropTypes.object
  };

  constructor(props: Props) {
    super(props);
    this.state = {
      branches: [],
      loading: true,
      query: '',
      selected: null
    };
  }

  componentDidMount() {
    this.mounted = true;
    this.fetchBranches();
    window.addEventListener('click', this.handleClickOutside);
  }

  componentWillUnmount() {
    this.mounted = false;
    window.removeEventListener('click', this.handleClickOutside);
  }

  fetchBranches = () => {
    this.setState({ loading: true });
    getBranches(this.props.project.key).then(
      (branches: Branch[]) => {
        if (this.mounted) {
          this.setState({ branches: sortBranchesAsTree(branches), loading: false });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  getFilteredBranches = () =>
    this.state.branches.filter(branch =>
      branch.name.toLowerCase().includes(this.state.query.toLowerCase())
    );

  handleClickOutside = (event: Event) => {
    if (!this.node || !this.node.contains(event.target as HTMLElement)) {
      this.props.onClose();
    }
  };

  handleSearchChange = (event: React.SyntheticEvent<HTMLInputElement>) =>
    this.setState({ query: event.currentTarget.value, selected: null });

  handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    switch (event.keyCode) {
      case 13:
        event.preventDefault();
        this.openSelected();
        return;
      case 27:
        event.preventDefault();
        this.props.onClose();
        return;
      case 38:
        event.preventDefault();
        this.selectPrevious();
        return;
      case 40:
        event.preventDefault();
        this.selectNext();
        return;
    }
  };

  openSelected = () => {
    const selected = this.getSelected();
    const branch = this.getFilteredBranches().find(branch => branch.name === selected);
    if (branch) {
      this.context.router.push(this.getProjectBranchUrl(branch));
    }
  };

  selectPrevious = () => {
    const selected = this.getSelected();
    const branches = this.getFilteredBranches();
    const index = branches.findIndex(branch => branch.name === selected);
    if (index > 0) {
      this.setState({ selected: branches[index - 1].name });
    }
  };

  selectNext = () => {
    const selected = this.getSelected();
    const branches = this.getFilteredBranches();
    const index = branches.findIndex(branch => branch.name === selected);
    if (index >= 0 && index < branches.length - 1) {
      this.setState({ selected: branches[index + 1].name });
    }
  };

  handleSelect = (branch: Branch) => {
    this.setState({ selected: branch.name });
  };

  getSelected = () => {
    const branches = this.getFilteredBranches();
    return this.state.selected || (branches.length > 0 && branches[0].name);
  };

  getProjectBranchUrl = (branch: Branch) => getProjectBranchUrl(this.props.project.key, branch);

  isSelected = (branch: Branch) => branch.name === this.getSelected();

  renderSearch = () =>
    <div className="search-box menu-search">
      <button className="search-box-submit button-clean">
        <i className="icon-search-new" />
      </button>
      <input
        autoFocus={true}
        className="search-box-input"
        onChange={this.handleSearchChange}
        onKeyDown={this.handleKeyDown}
        placeholder={translate('search_verb')}
        type="search"
        value={this.state.query}
      />
    </div>;

  renderBranchesList = () => {
    const branches = this.getFilteredBranches();
    const selected = this.getSelected();

    if (branches.length === 0) {
      return (
        <div className="menu-message note">
          {translate('no_results')}
        </div>
      );
    }

    const menu: JSX.Element[] = [];
    branches.forEach((branch, index) => {
      const isOrphan = isShortLivingBranch(branch) && branch.isOrphan;
      const previous = index > 0 ? branches[index - 1] : null;
      const isPreviousOrphan = isShortLivingBranch(previous) ? previous.isOrphan : false;
      if (isLongLivingBranch(branch) || (isOrphan && !isPreviousOrphan)) {
        menu.push(<li key={`divider-${branch.name}`} className="divider" />);
      }
      menu.push(
        <ComponentNavBranchesMenuItem
          branch={branch}
          component={this.props.project}
          key={branch.name}
          onSelect={this.handleSelect}
          selected={branch.name === selected}
        />
      );
    });

    return (
      <ul className="menu">
        {menu}
      </ul>
    );
  };

  render() {
    return (
      <div className="dropdown-menu dropdown-menu-shadow" ref={node => (this.node = node)}>
        {this.state.loading
          ? <i className="spinner" />
          : <div>
              {this.renderSearch()}
              {this.renderBranchesList()}
            </div>}
      </div>
    );
  }
}
