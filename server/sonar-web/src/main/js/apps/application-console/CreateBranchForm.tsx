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
import { some, without } from 'lodash';
import * as React from 'react';
import { ResetButtonLink, SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import SimpleModal from 'sonar-ui-common/components/controls/SimpleModal';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import MandatoryFieldMarker from 'sonar-ui-common/components/ui/MandatoryFieldMarker';
import MandatoryFieldsExplanation from 'sonar-ui-common/components/ui/MandatoryFieldsExplanation';
import { translate } from 'sonar-ui-common/helpers/l10n';
import {
  addApplicationBranch,
  getApplicationDetails,
  updateApplicationBranch
} from '../../api/application';
import { Application, ApplicationProject } from '../../types/application';
import ProjectBranchRow from './ProjectBranchRow';
import { ApplicationBranch, SelectBranchOption } from './utils';

interface Props {
  application: Application;
  branch?: ApplicationBranch;
  enabledProjectsKey: string[];
  onClose: () => void;
  onCreate?: (branch: ApplicationBranch) => void;
  onUpdate?: (name: string) => void;
}

interface BranchesList {
  [name: string]: SelectBranchOption | null;
}

interface State {
  loading: boolean;
  name: string;
  projects: ApplicationProject[];
  selected: string[];
  selectedBranches: BranchesList;
}

const MAX_PROJECTS_HEIGHT = 220;
const PROJECT_HEIGHT = 22;
export default class CreateBranchForm extends React.PureComponent<Props, State> {
  mounted = false;
  node?: HTMLElement | null = null;
  currentSelect?: HTMLElement | null = null;

  state: State = {
    loading: false,
    name: '',
    projects: [],
    selected: [],
    selectedBranches: {}
  };

  componentDidMount() {
    this.mounted = true;
    const { application } = this.props;
    const branch = this.props.branch ? this.props.branch.name : undefined;
    this.setState({ loading: true });
    getApplicationDetails(application.key, branch).then(
      ({ projects }) => {
        if (this.mounted) {
          const enabledProjects = projects.filter(p =>
            this.props.enabledProjectsKey.includes(p.key)
          );
          const selected = enabledProjects.filter(p => p.selected).map(p => p.key);
          const selectedBranches: BranchesList = {};
          enabledProjects.forEach(p => {
            if (!p.enabled) {
              selectedBranches[p.key] = null;
            } else {
              selectedBranches[p.key] = {
                value: p.branch || '',
                label: p.branch || '',
                isMain: p.isMain || false
              };
            }
          });
          this.setState({
            name: branch || '',
            selected,
            loading: false,
            projects: enabledProjects,
            selectedBranches
          });
        }
      },
      () => {
        this.props.onClose();
      }
    );
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  stopLoading = () => {
    if (this.mounted) {
      this.setState({ loading: false });
    }
  };

  canSubmit = () => {
    const hasUnselectedBranches = some(this.state.selectedBranches, (branch, projectKey) => {
      return !branch && this.state.selected.includes(projectKey);
    });
    return (
      !this.state.loading &&
      this.state.name.length > 0 &&
      !hasUnselectedBranches &&
      this.state.selected.length > 0
    );
  };

  handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ name: event.currentTarget.value });
  };

  handleFormSubmit = async () => {
    const projectKeys = this.state.selected;

    const projectBranches = projectKeys.map(p => {
      const branch = this.state.selectedBranches[p];
      return !branch || branch.isMain ? '' : branch.value;
    });

    if (this.props.branch) {
      await updateApplicationBranch({
        application: this.props.application.key,
        branch: this.props.branch.name,
        name: this.state.name,
        project: projectKeys,
        projectBranch: projectBranches
      });
      if (this.props.onUpdate) {
        this.props.onUpdate(this.state.name);
      }
    } else {
      await addApplicationBranch({
        application: this.props.application.key,
        branch: this.state.name,
        project: projectKeys,
        projectBranch: projectBranches
      });
      if (this.props.onCreate) {
        this.props.onCreate({ name: this.state.name, isMain: false });
      }
    }
    this.props.onClose();
  };

  handleProjectCheck = (checked: boolean, key: string) => {
    this.setState(state => ({
      selected: checked ? [...state.selected, key] : without(state.selected, key)
    }));
  };

  handleBranchChange = (projectKey: string, branch: SelectBranchOption) => {
    this.setState(state => ({
      selectedBranches: { ...state.selectedBranches, [projectKey]: branch }
    }));
  };

  handleSelectorClose = () => {
    if (this.node) {
      this.node.classList.add('selector-hidden');
    }
  };

  handleSelectorDirection = (selectNode: HTMLElement, elementCount: number) => {
    if (this.node) {
      const modalTop = this.node.getBoundingClientRect().top;
      const modalHeight = this.node.offsetHeight;
      const maxSelectHeight = Math.min(MAX_PROJECTS_HEIGHT, (elementCount + 1) * PROJECT_HEIGHT);
      const selectBottom = selectNode.getBoundingClientRect().top + maxSelectHeight;
      if (selectBottom > modalTop + modalHeight) {
        this.node.classList.add('inverted-direction');
      } else {
        this.node.classList.remove('inverted-direction');
      }
      this.node.classList.remove('selector-hidden');
    }
  };

  renderProjectsList = () => {
    return (
      <>
        <strong className="spacer-left spacer-top">
          {translate('application_console.branches.configuration')}
        </strong>
        <p className="spacer-top big-spacer-bottom spacer-left spacer-right">
          {translate('application_console.branches.create.help')}
        </p>
        <table className="data zebra">
          <thead>
            <tr>
              <th className="thin" />
              <th className="thin">{translate('project')}</th>
              <th>{translate('branch')}</th>
            </tr>
          </thead>
          <tbody>
            {this.state.projects.map(project => (
              <ProjectBranchRow
                checked={this.state.selected.includes(project.key)}
                key={project.key}
                onChange={this.handleBranchChange}
                onCheck={this.handleProjectCheck}
                onClose={this.handleSelectorClose}
                onOpen={this.handleSelectorDirection}
                project={project}
              />
            ))}
          </tbody>
        </table>
      </>
    );
  };

  render() {
    const isUpdating = this.props.branch !== undefined;
    const header = translate('application_console.branches', isUpdating ? 'update' : 'create');
    return (
      <SimpleModal
        header={header}
        onClose={this.props.onClose}
        onSubmit={this.handleFormSubmit}
        size="medium">
        {({ onCloseClick, onFormSubmit, submitting }) => (
          <form className="views-form" onSubmit={onFormSubmit}>
            <div className="modal-head">
              <h2>{header}</h2>
            </div>

            <div
              className="modal-body modal-container selector-hidden"
              ref={node => (this.node = node)}>
              {this.state.loading ? (
                <div className="text-center big-spacer-top big-spacer-bottom">
                  <i className="spinner spacer-right" />
                </div>
              ) : (
                <>
                  <MandatoryFieldsExplanation className="modal-field" />
                  <div className="modal-field">
                    <label htmlFor="view-edit-name">
                      {translate('name')}
                      <MandatoryFieldMarker />
                    </label>
                    <input
                      autoFocus={true}
                      className="input-super-large"
                      maxLength={250}
                      name="name"
                      onChange={this.handleInputChange}
                      size={50}
                      type="text"
                      value={this.state.name}
                    />
                  </div>
                  {this.renderProjectsList()}
                </>
              )}
            </div>

            <div className="modal-foot">
              <DeferredSpinner className="spacer-right" loading={submitting} />
              <SubmitButton disabled={submitting || !this.canSubmit()}>
                {translate(
                  'application_console.branches',
                  isUpdating ? 'update' : 'create',
                  'verb'
                )}
              </SubmitButton>
              <ResetButtonLink onClick={onCloseClick}>
                {translate('application_console.branches.cancel')}
              </ResetButtonLink>
            </div>
          </form>
        )}
      </SimpleModal>
    );
  }
}
