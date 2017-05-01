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
import React from 'react';
import { sortBy } from 'lodash';
import { TYPE, QUALIFIERS_ORDER } from './constants';
import DeleteView from './delete-view';
import BulkApplyTemplateView from './views/BulkApplyTemplateView';
import RadioToggle from '../../components/controls/RadioToggle';
import Checkbox from '../../components/controls/Checkbox';
import { translate } from '../../helpers/l10n';

export default class Search extends React.PureComponent {
  static propTypes = {
    onSearch: React.PropTypes.func.isRequired
  };

  onSubmit = e => {
    e.preventDefault();
    this.search();
  };

  search = () => {
    const q = this.refs.input.value;
    this.props.onSearch(q);
  };

  getTypeOptions = () => {
    return [
      { value: TYPE.ALL, label: 'All' },
      { value: TYPE.PROVISIONED, label: 'Provisioned' },
      { value: TYPE.GHOSTS, label: 'Ghosts' }
    ];
  };

  getQualifierOptions = () => {
    const options = this.props.topLevelQualifiers.map(q => {
      return { value: q, label: translate('qualifiers', q) };
    });
    return sortBy(options, option => QUALIFIERS_ORDER.indexOf(option.value));
  };

  onCheck = checked => {
    if (checked) {
      this.props.onAllSelected();
    } else {
      this.props.onAllDeselected();
    }
  };

  deleteProjects = () => {
    new DeleteView({
      deleteProjects: this.props.deleteProjects
    }).render();
  };

  bulkApplyTemplate = () => {
    new BulkApplyTemplateView({
      total: this.props.total,
      selection: this.props.selection,
      query: this.props.query,
      qualifier: this.props.qualifier,
      organization: this.props.organization
    }).render();
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

  renderGhostsDescription = () => {
    if (this.props.type !== TYPE.GHOSTS || !this.props.ready) {
      return null;
    }
    return (
      <div className="spacer-top alert alert-info">
        {translate('bulk_deletion.ghosts.description')}
      </div>
    );
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

  renderSpinner = () => <i className="spinner" />;

  render() {
    const isSomethingSelected = this.props.projects.length > 0 && this.props.selection.length > 0;
    return (
      <div className="panel panel-vertical bordered-bottom spacer-bottom">
        <table className="data">
          <tbody>
            <tr>
              <td className="thin text-middle">
                {this.props.ready ? this.renderCheckbox() : this.renderSpinner()}
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
                    ref="input"
                    className="search-box-input"
                    type="search"
                    placeholder="Search"
                  />
                </form>
              </td>
              <td className="thin nowrap text-middle">
                <button className="spacer-right" onClick={this.bulkApplyTemplate}>
                  {translate('permission_templates.bulk_apply_permission_template')}
                </button>
                <button
                  onClick={this.deleteProjects}
                  className="button-red"
                  disabled={!isSomethingSelected}>
                  {translate('delete')}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
        {this.renderGhostsDescription()}
      </div>
    );
  }
}
