/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import * as classNames from 'classnames';
import { debounce } from 'lodash';
import * as React from 'react';
import { ResetButtonLink, SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import { DropdownOverlay } from 'sonar-ui-common/components/controls/Dropdown';
import SearchBox from 'sonar-ui-common/components/controls/SearchBox';
import SimpleModal from 'sonar-ui-common/components/controls/SimpleModal';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { getSuggestions } from '../../../api/components';

interface Props {
  addedProjects: T.NotificationProject[];
  closeModal: VoidFunction;
  onSubmit: (project: T.NotificationProject) => void;
}

interface State {
  highlighted?: T.NotificationProject;
  loading?: boolean;
  query?: string;
  open?: boolean;
  selectedProject?: T.NotificationProject;
  suggestions?: T.NotificationProject[];
}

export default class ProjectModal extends React.PureComponent<Props, State> {
  mounted = false;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = {};
    this.handleSearch = debounce(this.handleSearch, 250);
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleKeyDown = (event: React.KeyboardEvent) => {
    switch (event.keyCode) {
      case 13:
        event.preventDefault();
        this.handleSelectHighlighted();
        break;
      case 38:
        event.preventDefault();
        this.handleHighlightPrevious();
        break;
      case 40:
        event.preventDefault();
        this.handleHighlightNext();
        break;
    }
  };

  getCurrentIndex = () => {
    const { highlighted, suggestions } = this.state;
    return highlighted && suggestions
      ? suggestions.findIndex(suggestion => suggestion.project === highlighted.project)
      : -1;
  };

  highlightIndex = (index: number) => {
    const { suggestions } = this.state;
    if (suggestions && suggestions.length > 0) {
      if (index < 0) {
        index = suggestions.length - 1;
      } else if (index >= suggestions.length) {
        index = 0;
      }
      this.setState({
        highlighted: suggestions[index]
      });
    }
  };

  handleHighlightPrevious = () => {
    this.highlightIndex(this.getCurrentIndex() - 1);
  };

  handleHighlightNext = () => {
    this.highlightIndex(this.getCurrentIndex() + 1);
  };

  handleSelectHighlighted = () => {
    const { highlighted, selectedProject } = this.state;
    if (highlighted !== undefined) {
      if (selectedProject !== undefined && highlighted.project === selectedProject.project) {
        this.handleSubmit();
      } else {
        this.handleSelect(highlighted);
      }
    }
  };

  handleSearch = (query: string) => {
    const { addedProjects } = this.props;

    if (query.length < 2) {
      this.setState({ open: false, query });
      return Promise.resolve([]);
    }

    this.setState({ loading: true, query });
    return getSuggestions(query).then(
      r => {
        if (this.mounted) {
          let suggestions = undefined;
          const projects = r.results.find(domain => domain.q === 'TRK');
          if (projects && projects.items.length > 0) {
            suggestions = projects.items
              .filter(item => !addedProjects.find(p => p.project === item.key))
              .map(item => ({
                project: item.key,
                projectName: item.name
              }));
          }
          this.setState({ loading: false, open: true, suggestions });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false, open: false });
        }
      }
    );
  };

  handleSelect = (selectedProject: T.NotificationProject) => {
    this.setState({
      open: false,
      query: selectedProject.projectName,
      selectedProject
    });
  };

  handleSubmit = () => {
    const { selectedProject } = this.state;
    if (selectedProject) {
      this.props.onSubmit(selectedProject);
    }
  };

  render() {
    const { closeModal } = this.props;
    const { highlighted, loading, query, open, selectedProject, suggestions } = this.state;
    const header = translate('my_account.set_notifications_for.title');
    return (
      <SimpleModal header={header} onClose={closeModal} onSubmit={this.handleSubmit}>
        {({ onCloseClick, onFormSubmit }) => (
          <form onSubmit={onFormSubmit}>
            <header className="modal-head">
              <h2>{header}</h2>
            </header>
            <div className="modal-body">
              <div className="modal-field abs-width-400">
                <label>{translate('my_account.set_notifications_for')}</label>
                <SearchBox
                  autoFocus={true}
                  onChange={this.handleSearch}
                  onKeyDown={this.handleKeyDown}
                  placeholder={translate('search.placeholder')}
                  value={query}
                />

                {loading && <i className="spinner spacer-left" />}

                {!loading && open && (
                  <div className="position-relative">
                    <DropdownOverlay className="abs-width-400" noPadding={true}>
                      {suggestions && suggestions.length > 0 ? (
                        <ul className="notifications-add-project-search-results">
                          {suggestions.map(suggestion => (
                            <li
                              className={classNames({
                                active: highlighted && highlighted.project === suggestion.project
                              })}
                              key={suggestion.project}
                              onClick={() => this.handleSelect(suggestion)}>
                              {suggestion.projectName}
                            </li>
                          ))}
                        </ul>
                      ) : (
                        <div className="notifications-add-project-no-search-results">
                          {translate('no_results')}
                        </div>
                      )}
                    </DropdownOverlay>
                  </div>
                )}
              </div>
            </div>
            <footer className="modal-foot">
              <div>
                <SubmitButton disabled={selectedProject === undefined}>
                  {translate('add_verb')}
                </SubmitButton>
                <ResetButtonLink onClick={onCloseClick}>{translate('cancel')}</ResetButtonLink>
              </div>
            </footer>
          </form>
        )}
      </SimpleModal>
    );
  }
}
