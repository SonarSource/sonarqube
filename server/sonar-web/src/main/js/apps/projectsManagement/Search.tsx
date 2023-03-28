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
import { sortBy } from 'lodash';
import * as React from 'react';
import { components, OptionProps, SingleValueProps } from 'react-select';
import { Project } from '../../api/components';
import withAppStateContext from '../../app/components/app-state/withAppStateContext';
import { Button } from '../../components/controls/buttons';
import Checkbox from '../../components/controls/Checkbox';
import DateInput from '../../components/controls/DateInput';
import HelpTooltip from '../../components/controls/HelpTooltip';
import SearchBox from '../../components/controls/SearchBox';
import Select from '../../components/controls/Select';
import QualifierIcon from '../../components/icons/QualifierIcon';
import { translate } from '../../helpers/l10n';
import { AppState } from '../../types/appstate';
import { Visibility } from '../../types/types';
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
  organization: string;
}

interface State {
  bulkApplyTemplateModal: boolean;
  deleteModal: boolean;
}

const QUALIFIERS_ORDER = ['TRK', 'VW', 'APP'];

export class Search extends React.PureComponent<Props, State> {
  mounted = false;
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

  handleQualifierChange = ({ value }: { value: string }) => this.props.onQualifierChanged(value);

  handleVisibilityChange = ({ value }: { value: string }) => this.props.onVisibilityChanged(value);

  optionRenderer = (props: OptionProps<{ value: string }, false>) => (
    <components.Option {...props}>{this.renderQualifierOption(props.data)}</components.Option>
  );

  singleValueRenderer = (props: SingleValueProps<{ value: string }>) => (
    <components.SingleValue {...props}>
      {this.renderQualifierOption(props.data as { value: string; label: string })}
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
        checked={checked}
        id="projects-selection"
        onCheck={this.onCheck}
        thirdState={thirdState}
        title={checked ? translate('uncheck_all') : translate('check_all')}
      />
    );
  };

  renderQualifierOption = (option: { label: string; value: string }) => (
    <div className="display-flex-center">
      <QualifierIcon className="little-spacer-right" qualifier={option.value} />
      {option.label}
    </div>
  );

  renderQualifierFilter = () => {
    const options = this.getQualifierOptions();
    if (options.length < 2) {
      return null;
    }
    return (
      <td className="thin nowrap text-middle">
        <Select
          className="input-medium it__project-qualifier-select"
          isDisabled={!this.props.ready}
          name="projects-qualifier"
          onChange={this.handleQualifierChange}
          isSearchable={false}
          components={{
            Option: this.optionRenderer,
            SingleValue: this.singleValueRenderer,
          }}
          options={this.getQualifierOptions()}
          value={options.find((option) => option.value === this.props.qualifiers)}
        />
      </td>
    );
  };

  renderVisibilityFilter = () => {
    const options = [
      { value: 'all', label: translate('visibility.both') },
      { value: 'public', label: translate('visibility.public') },
      { value: 'private', label: translate('visibility.private') },
    ];
    return (
      <td className="thin nowrap text-middle">
        <Select
          className="input-small"
          isDisabled={!this.props.ready}
          name="projects-visibility"
          onChange={this.handleVisibilityChange}
          options={options}
          isSearchable={false}
          value={options.find((option) => option.value === (this.props.visibility || 'all'))}
        />
      </td>
    );
  };

  renderTypeFilter = () =>
    this.props.qualifiers === 'TRK' ? (
      <td className="thin nowrap text-middle">
        <Checkbox
          checked={this.props.provisioned}
          className="link-checkbox-control"
          id="projects-provisioned"
          onCheck={this.props.onProvisionedChanged}
        >
          <span className="text-middle little-spacer-left">
            {translate('provisioning.only_provisioned')}
          </span>
        </Checkbox>
        <HelpTooltip
          className="spacer-left"
          overlay={translate('provisioning.only_provisioned.tooltip')}
        />
      </td>
    ) : null;

  renderDateFilter = () => {
    return (
      <td className="thin nowrap text-middle">
        <DateInput
          inputClassName="input-medium"
          name="analyzed-before"
          onChange={this.props.onDateChanged}
          placeholder={translate('last_analysis_before')}
          value={this.props.analyzedBefore}
        />
      </td>
    );
  };

  render() {
    return (
      <div className="big-spacer-bottom">
        <table className="data">
          <tbody>
            <tr>
              <td className="thin text-middle">
                {this.props.ready ? this.renderCheckbox() : <i className="spinner" />}
              </td>
              {this.renderQualifierFilter()}
              {this.renderDateFilter()}
              {this.renderVisibilityFilter()}
              {this.renderTypeFilter()}
              <td className="text-middle">
                <SearchBox
                  minLength={3}
                  onChange={this.props.onSearch}
                  placeholder={translate('search.search_by_name_or_key')}
                  value={this.props.query}
                />
              </td>
              <td className="thin nowrap text-middle">
                <Button
                  className="js-bulk-apply-permission-template"
                  disabled={this.props.selection.length === 0}
                  onClick={this.handleBulkApplyTemplateClick}
                >
                  {translate('permission_templates.bulk_apply_permission_template')}
                </Button>
                {this.props.qualifiers === 'TRK' && (
                  <Button
                    className="js-delete spacer-left button-red"
                    disabled={this.props.selection.length === 0}
                    onClick={this.handleDeleteClick}
                    title={
                      this.props.selection.length === 0
                        ? translate('permission_templates.select_to_delete')
                        : translate('permission_templates.delete_selected')
                    }
                  >
                    {translate('delete')}
                  </Button>
                )}
              </td>
            </tr>
          </tbody>
        </table>

        {this.state.bulkApplyTemplateModal && (
          <BulkApplyTemplateModal
            organization={this.props.organization}
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
            organization={this.props.organization}
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
