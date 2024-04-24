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

import {
  ButtonSecondary,
  Checkbox,
  DangerButtonPrimary,
  DatePicker,
  HelperHintIcon,
  InputSearch,
  InputSelect,
  Spinner,
} from 'design-system';
import { sortBy } from 'lodash';
import * as React from 'react';
import { OptionProps, SingleValueProps, components } from 'react-select';
import HelpTooltip from '~sonar-aligned/components/controls/HelpTooltip';
import { Project } from '../../api/project-management';
import withAppStateContext from '../../app/components/app-state/withAppStateContext';
import { translate } from '../../helpers/l10n';
import { LabelValueSelectOption } from '../../helpers/search';
import { AppState } from '../../types/appstate';
import { Visibility } from '../../types/component';
import BulkApplyTemplateModal from './BulkApplyTemplateModal';
import DeleteModal from './DeleteModal';

export interface Props {
  analyzedBefore: Date | undefined;
  onAllDeselected: () => void;
  onAllSelected: () => void;
  onDateChanged: (analyzedBefore: Date | undefined) => void;
  onDeleteProjects: () => void;
  onProvisionedChanged: (provisioned: boolean) => void;
  onQualifierChanged: (qualifier: string) => void;
  onVisibilityChanged: (qualifier: string) => void;
  onSearch: (query: string) => void;
  projects: Project[];
  provisioned: boolean;
  qualifiers: string;
  query: string;
  ready: boolean;
  selection: any[];
  appState: AppState;
  total: number;
  visibility?: Visibility;
}

interface State {
  bulkApplyTemplateModal: boolean;
  deleteModal: boolean;
}

const QUALIFIERS_ORDER = ['TRK', 'VW', 'APP'];

class Search extends React.PureComponent<Props, State> {
  state: State = { bulkApplyTemplateModal: false, deleteModal: false };

  getQualifierOptions = () => {
    const options = this.props.appState.qualifiers.map((q) => ({
      label: translate('qualifiers', q),
      value: q,
    }));
    return sortBy(options, (option) => QUALIFIERS_ORDER.indexOf(option.value));
  };

  onCheck = (checked: boolean) => {
    if (checked) {
      this.props.onAllSelected();
    } else {
      this.props.onAllDeselected();
    }
  };

  handleDeleteClick = () => {
    this.setState({ deleteModal: true });
  };

  closeDeleteModal = () => {
    this.setState({ deleteModal: false });
  };

  handleDeleteConfirm = () => {
    this.closeDeleteModal();
    this.props.onDeleteProjects();
  };

  handleBulkApplyTemplateClick = () => {
    this.setState({ bulkApplyTemplateModal: true });
  };

  closeBulkApplyTemplateModal = () => {
    this.setState({ bulkApplyTemplateModal: false });
  };

  handleQualifierChange = ({ value }: LabelValueSelectOption) =>
    this.props.onQualifierChanged(value);

  handleVisibilityChange = ({ value }: LabelValueSelectOption) =>
    this.props.onVisibilityChanged(value);

  optionRenderer = (props: OptionProps<LabelValueSelectOption, false>) => (
    <components.Option {...props}>{this.renderQualifierOption(props.data)}</components.Option>
  );

  singleValueRenderer = (props: SingleValueProps<LabelValueSelectOption, false>) => (
    <components.SingleValue {...props}>
      {this.renderQualifierOption(props.data)}
    </components.SingleValue>
  );

  renderCheckbox = () => {
    const isAllChecked =
      this.props.projects.length > 0 && this.props.selection.length === this.props.projects.length;
    const thirdState =
      this.props.projects.length > 0 &&
      this.props.selection.length > 0 &&
      this.props.selection.length < this.props.projects.length;
    const checked = isAllChecked || thirdState;
    return (
      <Checkbox
        className="it__projects-selection"
        checked={checked}
        id="projects-selection"
        onCheck={this.onCheck}
        thirdState={thirdState}
        title={checked ? translate('uncheck_all') : translate('check_all')}
      />
    );
  };

  renderQualifierOption = (option: LabelValueSelectOption) => <div>{option.label}</div>;

  renderQualifierFilter = () => {
    const options = this.getQualifierOptions();
    if (options.length < 2) {
      return null;
    }
    return (
      <InputSelect
        className="it__project-qualifier-select"
        isDisabled={!this.props.ready}
        name="projects-qualifier"
        onChange={this.handleQualifierChange}
        isSearchable={false}
        components={{
          Option: this.optionRenderer,
          SingleValue: this.singleValueRenderer,
        }}
        options={this.getQualifierOptions()}
        aria-label={translate('projects_management.filter_by_component')}
        value={options.find((option) => option.value === this.props.qualifiers)}
      />
    );
  };

  renderVisibilityFilter = () => {
    const options = [
      { value: 'all', label: translate('visibility.both') },
      { value: Visibility.Public, label: translate('visibility.public') },
      { value: Visibility.Private, label: translate('visibility.private') },
    ];
    return (
      <InputSelect
        isDisabled={!this.props.ready}
        name="projects-visibility"
        onChange={this.handleVisibilityChange}
        options={options}
        isSearchable={false}
        aria-label={translate('projects_management.filter_by_visibility')}
        value={options.find((option) => option.value === (this.props.visibility || 'all'))}
      />
    );
  };

  renderTypeFilter = () =>
    this.props.qualifiers === 'TRK' ? (
      <div className="sw-flex sw-items-center">
        <Checkbox
          checked={this.props.provisioned}
          id="projects-provisioned"
          onCheck={this.props.onProvisionedChanged}
        >
          <span className="sw-ml-1">{translate('provisioning.only_provisioned')}</span>
          <HelpTooltip
            className="sw-ml-2"
            overlay={translate('provisioning.only_provisioned.tooltip')}
          >
            <HelperHintIcon />
          </HelpTooltip>
        </Checkbox>
      </div>
    ) : null;

  renderDateFilter = () => {
    return (
      <DatePicker
        clearButtonLabel={translate('clear')}
        name="analyzed-before"
        onChange={this.props.onDateChanged}
        placeholder={translate('last_analysis_before')}
        value={this.props.analyzedBefore}
        showClearButton
        alignRight
        size="auto"
      />
    );
  };

  render() {
    return (
      <div className="sw-mb-4">
        <div className="sw-flex sw-justify-start sw-items-center sw-flex-wrap sw-gap-2 sw-p-2">
          <Spinner loading={!this.props.ready} className="sw-ml-2">
            {this.renderCheckbox()}
          </Spinner>
          {this.renderQualifierFilter()}
          {this.renderDateFilter()}
          {this.renderVisibilityFilter()}
          {this.renderTypeFilter()}
          <div className="sw-flex-grow">
            <InputSearch
              minLength={3}
              onChange={this.props.onSearch}
              placeholder={translate('search.search_by_name_or_key')}
              value={this.props.query}
              size="auto"
            />
          </div>
          <div>
            <ButtonSecondary
              className="it__bulk-apply-permission-template"
              disabled={this.props.selection.length === 0}
              onClick={this.handleBulkApplyTemplateClick}
            >
              {translate('permission_templates.bulk_apply_permission_template')}
            </ButtonSecondary>
            {this.props.qualifiers === 'TRK' && (
              <DangerButtonPrimary
                className="sw-ml-2"
                disabled={this.props.selection.length === 0}
                onClick={this.handleDeleteClick}
                title={
                  this.props.selection.length === 0
                    ? translate('permission_templates.select_to_delete')
                    : translate('permission_templates.delete_selected')
                }
              >
                {translate('delete')}
              </DangerButtonPrimary>
            )}
          </div>
        </div>

        {this.state.bulkApplyTemplateModal && (
          <BulkApplyTemplateModal
            analyzedBefore={this.props.analyzedBefore}
            onClose={this.closeBulkApplyTemplateModal}
            provisioned={this.props.provisioned}
            qualifier={this.props.qualifiers}
            query={this.props.query}
            selection={this.props.selection}
            total={this.props.total}
          />
        )}

        {this.state.deleteModal && (
          <DeleteModal
            analyzedBefore={this.props.analyzedBefore}
            onClose={this.closeDeleteModal}
            onConfirm={this.handleDeleteConfirm}
            provisioned={this.props.provisioned}
            qualifier={this.props.qualifiers}
            query={this.props.query}
            selection={this.props.selection}
            total={this.props.total}
          />
        )}
      </div>
    );
  }
}

export default withAppStateContext(Search);
