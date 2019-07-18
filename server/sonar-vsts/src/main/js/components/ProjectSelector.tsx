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
import { Component } from '@sqapi/components';
import * as classNames from 'classnames';
import { debounce } from 'lodash';
import * as React from 'react';
import ProjectSelectorItem from './ProjectSelectorItem';

interface Props {
  isLoggedIn: boolean;
  onQueryChange: (query: string) => Promise<void>;
  onSelect: (component: Component) => void;
  projects: Component[];
  selected?: Component;
}

interface State {
  activeIdx: number;
  activeKey?: string;
  favorite: boolean;
  open: boolean;
  search: string;
  searching: boolean;
}

export default class ProjectSelector extends React.PureComponent<Props, State> {
  node?: HTMLElement | null;
  debouncedHandleSearch: () => void;

  constructor(props: Props) {
    super(props);
    const firstProject = props.projects[0];
    this.state = {
      activeIdx: firstProject ? 0 : -1,
      activeKey: firstProject && firstProject.key,
      favorite: props.isLoggedIn,
      open: false,
      search: '',
      searching: false
    };
    this.debouncedHandleSearch = debounce(this.handleSearch, 250);
  }

  componentDidMount() {
    window.addEventListener('click', this.handleClickOutside);
    if (this.node) {
      this.node.addEventListener('keydown', this.handleKeyDown, true);
    }
  }

  componentWillReceiveProps(nextProps: Props) {
    if (this.props.projects !== nextProps.projects) {
      let activeIdx = nextProps.projects.findIndex(project => project.key === this.state.activeKey);
      activeIdx = activeIdx >= 0 ? activeIdx : 0;
      this.setState({ activeIdx, activeKey: this.getActiveKey(activeIdx) });
    }
  }

  componentWillUnmount() {
    window.removeEventListener('click', this.handleClickOutside);
    if (this.node) {
      this.node.removeEventListener('keydown', this.handleKeyDown);
    }
  }

  getActiveKey = (idx: number) => {
    const { projects } = this.props;
    return projects[idx] && projects[idx].key;
  };

  getEmptyMessage = () => {
    const { favorite, search } = this.state;
    if (search) {
      return 'No project matching your search.';
    } else if (favorite) {
      return "You don't have any favorite projects yet.";
    }
    return 'No project have been found';
  };

  handleClickOutside = (event: Event) => {
    if (!this.node || !this.node.contains(event.target as HTMLElement)) {
      this.setState({ open: false });
    }
  };

  handleFilterAll = () => {
    this.setState({ favorite: false, searching: true }, this.handleSearch);
  };

  handleFilterFavorite = () => {
    this.setState({ favorite: true, searching: true }, this.handleSearch);
  };

  handleItemHover = (item: Component) => {
    let activeIdx = this.props.projects.findIndex(project => project.key === item.key);
    activeIdx = activeIdx >= 0 ? activeIdx : 0;
    this.setState({ activeIdx, activeKey: this.getActiveKey(activeIdx) });
  };

  handleKeyDown = (evt: KeyboardEvent) => {
    switch (evt.keyCode) {
      case 40: // down
        evt.stopPropagation();
        evt.preventDefault();
        this.setState(this.selectNextItem);
        break;
      case 38: // up
        evt.stopPropagation();
        evt.preventDefault();
        this.setState(this.selectPreviousItem);
        break;
      case 37: // left
      case 39: // right
        evt.stopPropagation();
        break;
      case 13: // enter
        if (this.state.activeIdx >= 0) {
          this.handleSelect(this.props.projects[this.state.activeIdx]);
        }
        break;
      case 27: // escape
        this.setState({ open: false });
        break;
    }
  };

  handleSearchChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState(
      { search: event.currentTarget.value, searching: true },
      this.debouncedHandleSearch
    );
  };

  handleSearch = () => {
    const filter = [];
    if (this.state.favorite) {
      filter.push('isFavorite');
    }
    if (this.state.search) {
      filter.push(`query = "${this.state.search}"`);
    }
    this.props.onQueryChange(filter.join(' and ')).then(this.stopSearching, this.stopSearching);
  };

  handleSelect = (project: Component) => {
    this.props.onSelect(project);
    this.setState({ open: false });
  };

  selectNextItem = ({ activeIdx }: State, { projects }: Props) => {
    let newActiveIdx = activeIdx + 1;
    if (activeIdx < 0 || activeIdx >= projects.length - 1) {
      newActiveIdx = 0;
    }
    return { activeIdx: newActiveIdx, activeKey: this.getActiveKey(newActiveIdx) };
  };

  selectPreviousItem = ({ activeIdx }: State, { projects }: Props) => {
    let newActiveIdx = activeIdx - 1;
    if (activeIdx <= 0) {
      newActiveIdx = projects.length - 1;
    }
    return { activeIdx: newActiveIdx, activeKey: this.getActiveKey(newActiveIdx) };
  };

  stopSearching = () => {
    this.setState({ searching: false });
  };

  toggleOpen = () => {
    this.setState(({ open }) => ({ open: !open }));
  };

  render() {
    const { isLoggedIn, projects, selected } = this.props;
    const { activeIdx, favorite, open, search, searching } = this.state;
    return (
      <div className="project-picker" ref={node => (this.node = node)}>
        <div
          className="filtered-list-dropdown-menu"
          onClick={this.toggleOpen}
          role="button"
          tabIndex={0}>
          <span className="selected-item-text">
            {selected ? selected.name : 'Select a project...'}
          </span>
          <span className="drop-icon bowtie-icon bowtie-chevron-down-light" />
        </div>
        {open && (
          <div className="filtered-list-popup" role="dialog">
            <div className="filtered-list-control bowtie-filtered-list">
              <div className="filter-container">
                {isLoggedIn && (
                  <div className="views">
                    <ul className="pivot-view" role="tablist">
                      <li
                        className={classNames('filtered-list-tab', { selected: favorite })}
                        role="presentation">
                        <a onClick={this.handleFilterFavorite} role="tab" tabIndex={0}>
                          My Projects
                        </a>
                      </li>
                      <li
                        className={classNames('filtered-list-tab', { selected: !favorite })}
                        role="presentation">
                        <a onClick={this.handleFilterAll} role="tab" tabIndex={-1}>
                          All
                        </a>
                      </li>
                    </ul>
                  </div>
                )}
                <div className="filtered-list-search-container bowtie-style">
                  <input
                    autoFocus={true}
                    className="filtered-list-search"
                    onChange={this.handleSearchChange}
                    placeholder="Search by project name"
                    type="text"
                    value={search}
                  />
                  {searching && <i className="spinner" />}
                </div>
              </div>
              <ul className="filtered-list">
                {projects.map((project, idx) => (
                  <ProjectSelectorItem
                    isActive={activeIdx === idx}
                    isSelected={Boolean(selected && selected.key === project.key)}
                    key={project.key}
                    onHover={this.handleItemHover}
                    onSelect={this.handleSelect}
                    project={project}
                  />
                ))}
                {projects.length <= 0 && (
                  <li className="filtered-list-message">{this.getEmptyMessage()}</li>
                )}
              </ul>
            </div>
          </div>
        )}
      </div>
    );
  }
}
