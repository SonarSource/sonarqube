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

import { Button, ButtonVariety } from '@sonarsource/echoes-react';
import * as React from 'react';
import {
  DropdownMenu,
  InputSearch,
  ItemButton,
  Modal,
  Popup,
  PopupPlacement,
  PopupZLevel,
  Spinner,
} from '~design-system';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { getSuggestions } from '../../../api/components';
import { KeyboardKeys } from '../../../helpers/keycodes';
import { translate } from '../../../helpers/l10n';
import { NotificationProject } from '../../../types/notifications';

interface Props {
  addedProjects: NotificationProject[];
  closeModal: VoidFunction;
  onSubmit: (project: NotificationProject) => void;
}

interface State {
  highlighted?: NotificationProject;
  loading?: boolean;
  query?: string;
  selectedProject?: NotificationProject;
  suggestions?: NotificationProject[];
}

export default class ProjectModal extends React.PureComponent<Props, State> {
  mounted = false;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = {};
  }

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleKeyDown = (event: React.KeyboardEvent) => {
    switch (event.nativeEvent.key) {
      case KeyboardKeys.Enter:
        event.preventDefault();
        this.handleSelectHighlighted();
        break;

      case KeyboardKeys.UpArrow:
        event.preventDefault();
        this.handleHighlightPrevious();
        break;

      case KeyboardKeys.DownArrow:
        event.preventDefault();
        this.handleHighlightNext();
        break;
    }
  };

  getCurrentIndex = () => {
    const { highlighted, suggestions } = this.state;

    return highlighted && suggestions
      ? suggestions.findIndex((suggestion) => suggestion.project === highlighted.project)
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
        highlighted: suggestions[index],
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
    const { highlighted } = this.state;

    if (highlighted !== undefined) {
      this.handleSelect(highlighted);
    }
  };

  handleSearch = (query: string) => {
    const { addedProjects } = this.props;

    if (query.length < 2) {
      this.setState({ query, selectedProject: undefined, suggestions: undefined });

      return;
    }

    this.setState({ loading: true, query, selectedProject: undefined });

    getSuggestions(query).then(
      (r) => {
        if (this.mounted) {
          let suggestions = undefined;

          const projects = r.results.find((domain) => domain.q === ComponentQualifier.Project);

          if (projects && projects.items.length > 0) {
            suggestions = projects.items
              .filter((item) => !addedProjects.find((p) => p.project === item.key))
              .map((item) => ({
                project: item.key,
                projectName: item.name,
              }));
          }

          this.setState({ loading: false, suggestions });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      },
    );
  };

  handleSelect = (selectedProject: NotificationProject) => {
    this.setState({
      query: selectedProject.projectName,
      selectedProject,
      suggestions: undefined,
    });
  };

  handleSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    const { selectedProject } = this.state;

    if (selectedProject) {
      this.props.onSubmit(selectedProject);
    }
  };

  render() {
    const { closeModal } = this.props;
    const { highlighted, loading, query, selectedProject, suggestions } = this.state;

    const projectSuggestion = (suggestion: NotificationProject) => (
      <ItemButton
        className="sw-my-1"
        key={suggestion.project}
        onClick={() => this.handleSelect(suggestion)}
        selected={
          highlighted?.project === suggestion.project ||
          selectedProject?.project === suggestion.project
        }
      >
        {suggestion.projectName}
      </ItemButton>
    );

    const isSearching = query?.length && !selectedProject;

    const noResults = isSearching ? (
      <div className="sw-mx-5 sw-my-3">{translate('no_results')}</div>
    ) : undefined;

    return (
      <Modal
        body={
          <form id="project-notifications-modal-form" onSubmit={this.handleSubmit}>
            <Popup
              allowResizing
              overlay={
                isSearching ? (
                  <DropdownMenu
                    className="sw-overflow-x-hidden sw-min-w-abs-350"
                    maxHeight="38rem"
                    size="auto"
                  >
                    <Spinner className="sw-mx-5 sw-my-3" loading={!!loading}>
                      {suggestions && suggestions.length > 0 ? (
                        <ul className="sw-py-2">
                          {suggestions.map((suggestion) => projectSuggestion(suggestion))}
                        </ul>
                      ) : (
                        noResults
                      )}
                    </Spinner>
                  </DropdownMenu>
                ) : undefined
              }
              placement={PopupPlacement.BottomLeft}
              zLevel={PopupZLevel.Global}
            >
              <InputSearch
                autoFocus
                className="sw-my-2"
                onChange={this.handleSearch}
                onKeyDown={this.handleKeyDown}
                placeholder={translate('my_account.set_notifications_for')}
                searchInputAriaLabel={translate('search_verb')}
                size="full"
                value={query}
              />
            </Popup>
          </form>
        }
        headerTitle={translate('my_account.set_notifications_for.title')}
        onClose={closeModal}
        primaryButton={
          <Button
            isDisabled={selectedProject === undefined}
            form="project-notifications-modal-form"
            type="submit"
            variety={ButtonVariety.Primary}
          >
            {translate('add_verb')}
          </Button>
        }
        secondaryButtonLabel={translate('cancel')}
      />
    );
  }
}
