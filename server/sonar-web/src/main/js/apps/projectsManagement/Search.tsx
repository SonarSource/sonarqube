/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import * as Select from 'react-select';
import { sortBy } from 'lodash';
import BulkApplyTemplateModal from './BulkApplyTemplateModal';
import DeleteModal from './DeleteModal';
import { QUALIFIERS_ORDER } from './utils';
import { Project } from './utils';
import { Organization } from '../../app/types';
import Checkbox from '../../components/controls/Checkbox';
import { translate } from '../../helpers/l10n';
import QualifierIcon from '../../components/shared/QualifierIcon';
import Tooltip from '../../components/controls/Tooltip';
import DateInput from '../../components/controls/DateInput';

export interface Props {
  analyzedBefore?: string;
  onAllDeselected: () => void;
  onAllSelected: () => void;
  onDateChanged: (analyzedBefore?: string) => void;
  onDeleteProjects: () => void;
  onProvisionedChanged: (provisioned: boolean) => void;
  onQualifierChanged: (qualifier: string) => void;
  onSearch: (query: string) => void;
  organization: Organization;
  projects: Project[];
  provisioned: boolean;
  qualifiers: string;
  query: string;
  ready: boolean;
  selection: any[];
  topLevelQualifiers: string[];
  total: number;
}

interface State {
  bulkApplyTemplateModal: boolean;
  deleteModal: boolean;
}

export default class Search extends React.PureComponent<Props, State> {
  input: HTMLInputElement;
  mounted: boolean;
  state: State = { bulkApplyTemplateModal: false, deleteModal: false };

  onSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    this.search();
  };

  search = (event?: React.SyntheticEvent<HTMLInputElement>) => {
    const q = event ? event.currentTarget.value : this.input.value;
    this.props.onSearch(q);
  };

  getQualifierOptions = () => {
    const options = this.props.topLevelQualifiers.map(q => ({
      label: translate('qualifiers', q),
      value: q
    }));
    return sortBy(options, option => QUALIFIERS_ORDER.indexOf(option.value));
  };

  onCheck = (checked: boolean) => {
    if (checked) {
      this.props.onAllSelected();
    } else {
      this.props.onAllDeselected();
    }
  };

  handleDeleteClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.setState({ deleteModal: true });
  };

  closeDeleteModal = () => {
    this.setState({ deleteModal: false });
  };

  handleDeleteConfirm = () => {
    this.closeDeleteModal();
    this.props.onDeleteProjects();
  };

  handleBulkApplyTemplateClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.setState({ bulkApplyTemplateModal: true });
  };

  closeBulkApplyTemplateModal = () => {
    this.setState({ bulkApplyTemplateModal: false });
  };

  handleQualifierChange = ({ value }: { value: string }) => this.props.onQualifierChanged(value);

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
        thirdState={thirdState}
        onCheck={this.onCheck}
      />
    );
  };

  renderQualifierOption = (option: { label: string; value: string }) =>
    <span>
      <QualifierIcon className="little-spacer-right" qualifier={option.value} />
      {option.label}
    </span>;

  renderQualifierFilter = () => {
    const options = this.getQualifierOptions();
    if (options.length < 2) {
      return null;
    }
    return (
      <td className="thin nowrap text-middle">
        <Select
          className="input-medium"
          clearable={false}
          disabled={!this.props.ready}
          optionRenderer={this.renderQualifierOption}
          options={this.getQualifierOptions()}
          value={this.props.qualifiers}
          valueRenderer={this.renderQualifierOption}
          name="projects-qualifier"
          onChange={this.handleQualifierChange}
          searchable={false}
        />
      </td>
    );
  };

  renderTypeFilter = () =>
    this.props.qualifiers === 'TRK'
      ? <td className="thin nowrap text-middle">
          <Checkbox
            className="link-checkbox-control"
            checked={this.props.provisioned}
            id="projects-provisioned"
            onCheck={this.props.onProvisionedChanged}>
            <span className="little-spacer-left">
              {translate('provisioning.only_provisioned')}
              <Tooltip overlay={translate('provisioning.only_provisioned.tooltip')}>
                <i className="spacer-left icon-help" />
              </Tooltip>
            </span>
          </Checkbox>
        </td>
      : null;

  renderDateFilter = () => {
    return (
      <td className="thin nowrap text-middle">
        <DateInput
          inputClassName="input-medium"
          name="analyzed-before"
          onChange={this.props.onDateChanged}
          placeholder={translate('analyzed_before')}
          value={this.props.analyzedBefore}
        />
      </td>
    );
  };

  render() {
    const isSomethingSelected = this.props.projects.length > 0 && this.props.selection.length > 0;
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
              {this.renderTypeFilter()}
              <td className="text-middle">
                <form onSubmit={this.onSubmit} className="search-box">
                  <button className="search-box-submit button-clean">
                    <i className="icon-search" />
                  </button>
                  <input
                    onChange={this.search}
                    value={this.props.query}
                    ref={node => (this.input = node!)}
                    className="search-box-input input-medium"
                    type="search"
                    placeholder="Search"
                  />
                </form>
              </td>
              <td className="thin nowrap text-middle">
                <button
                  className="spacer-right js-bulk-apply-permission-template"
                  onClick={this.handleBulkApplyTemplateClick}>
                  {translate('permission_templates.bulk_apply_permission_template')}
                </button>
                <button
                  onClick={this.handleDeleteClick}
                  className="js-delete button-red"
                  disabled={!isSomethingSelected}>
                  {translate('delete')}
                </button>
              </td>
            </tr>
          </tbody>
        </table>

        {this.state.bulkApplyTemplateModal &&
          <BulkApplyTemplateModal
            onClose={this.closeBulkApplyTemplateModal}
            organization={this.props.organization.key}
            provisioned={this.props.provisioned}
            qualifier={this.props.qualifiers}
            query={this.props.query}
            selection={this.props.selection}
            total={this.props.total}
          />}

        {this.state.deleteModal &&
          <DeleteModal
            onClose={this.closeDeleteModal}
            onConfirm={this.handleDeleteConfirm}
            organization={this.props.organization.key}
            qualifier={this.props.qualifiers}
            selection={this.props.selection}
          />}
      </div>
    );
  }
}
