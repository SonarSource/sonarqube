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
import { sortBy } from 'lodash';
import BulkApplyTemplateModal from './BulkApplyTemplateModal';
import DeleteModal from './DeleteModal';
import { Type, QUALIFIERS_ORDER } from './utils';
import { Project } from './utils';
import { Organization } from '../../app/types';
import RadioToggle from '../../components/controls/RadioToggle';
import Checkbox from '../../components/controls/Checkbox';
import { translate } from '../../helpers/l10n';

export interface Props {
  onAllDeselected: () => void;
  onAllSelected: () => void;
  onDeleteProjects: () => void;
  onQualifierChanged: (qualifier: string) => void;
  onSearch: (query: string) => void;
  onTypeChanged: (type: Type) => void;
  organization: Organization;
  projects: Project[];
  qualifiers: string;
  query: string;
  ready: boolean;
  selection: any[];
  topLevelQualifiers: string[];
  total: number;
  type: Type;
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

  getTypeOptions = () => [
    { value: Type.All, label: 'All' },
    { value: Type.Provisioned, label: 'Provisioned' }
  ];

  getQualifierOptions = () => {
    const options = this.props.topLevelQualifiers.map(q => {
      return { value: q, label: translate('qualifiers', q) };
    });
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

  renderCheckbox = () => {
    const isAllChecked =
      this.props.projects.length > 0 && this.props.selection.length === this.props.projects.length;
    const thirdState =
      this.props.projects.length > 0 &&
      this.props.selection.length > 0 &&
      this.props.selection.length < this.props.projects.length;
    const checked = isAllChecked || thirdState;
    return <Checkbox checked={checked} thirdState={thirdState} onCheck={this.onCheck} />;
  };

  renderQualifierFilter = () => {
    const options = this.getQualifierOptions();
    if (options.length < 2) {
      return null;
    }
    return (
      <td className="thin nowrap text-middle">
        <RadioToggle
          options={this.getQualifierOptions()}
          value={this.props.qualifiers}
          name="projects-qualifier"
          onCheck={this.props.onQualifierChanged}
        />
      </td>
    );
  };

  render() {
    const isSomethingSelected = this.props.projects.length > 0 && this.props.selection.length > 0;
    return (
      <div className="panel panel-vertical bordered-bottom spacer-bottom">
        <table className="data">
          <tbody>
            <tr>
              <td className="thin text-middle">
                {this.props.ready ? this.renderCheckbox() : <i className="spinner" />}
              </td>
              {this.renderQualifierFilter()}
              <td className="thin nowrap text-middle">
                <RadioToggle
                  options={this.getTypeOptions()}
                  value={this.props.type}
                  name="projects-type"
                  onCheck={this.props.onTypeChanged}
                />
              </td>
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
            qualifier={this.props.qualifiers}
            query={this.props.query}
            selection={this.props.selection}
            total={this.props.total}
            type={this.props.type}
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
