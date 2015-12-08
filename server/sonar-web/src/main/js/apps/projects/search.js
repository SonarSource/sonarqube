import _ from 'underscore';
import React from 'react';
import { TYPE, QUALIFIERS_ORDER } from './constants';
import DeleteView from './delete-view';
import RadioToggle from '../../components/shared/radio-toggle';
import Checkbox from '../../components/shared/checkbox';

export default React.createClass({
  propTypes: {
    onSearch: React.PropTypes.func.isRequired
  },

  onSubmit(e) {
    e.preventDefault();
    this.search();
  },

  search() {
    let q = this.refs.input.value;
    this.props.onSearch(q);
  },

  getTypeOptions() {
    return [
      { value: TYPE.ALL, label: 'All' },
      { value: TYPE.PROVISIONED, label: 'Provisioned' },
      { value: TYPE.GHOSTS, label: 'Ghosts' }
    ];
  },

  getQualifierOptions() {
    let options = this.props.topLevelQualifiers.map(q => {
      return { value: q, label: window.t('qualifiers', q) };
    });
    return _.sortBy(options, option => {
      return QUALIFIERS_ORDER.indexOf(option.value);
    });
  },

  renderCheckbox() {
    let isAllChecked = this.props.projects.length > 0 &&
            this.props.selection.length === this.props.projects.length;
    let thirdState = this.props.projects.length > 0 &&
            this.props.selection.length > 0 &&
            this.props.selection.length < this.props.projects.length;
    let isChecked = isAllChecked || thirdState;
    return <Checkbox onCheck={this.onCheck} initiallyChecked={isChecked} thirdState={thirdState}/>;
  },

  renderSpinner() {
    return <i className="spinner"/>;
  },

  onCheck(checked) {
    if (checked) {
      this.props.onAllSelected();
    } else {
      this.props.onAllDeselected();
    }
  },

  renderGhostsDescription () {
    if (this.props.type !== TYPE.GHOSTS || !this.props.ready) {
      return null;
    }
    return <div className="spacer-top alert alert-info">{window.t('bulk_deletion.ghosts.description')}</div>;
  },

  deleteProjects() {
    new DeleteView({
      deleteProjects: this.props.deleteProjects
    }).render();
  },

  renderQualifierFilter() {
    let options = this.getQualifierOptions();
    if (options.length < 2) {
      return null;
    }
    return (
        <td className="thin nowrap text-middle">
          <RadioToggle options={this.getQualifierOptions()} value={this.props.qualifiers}
                       name="projects-qualifier" onCheck={this.props.onQualifierChanged}/>
        </td>
    );
  },

  render() {
    let isSomethingSelected = this.props.projects.length > 0 && this.props.selection.length > 0;
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
                <RadioToggle options={this.getTypeOptions()} value={this.props.type}
                             name="projects-type" onCheck={this.props.onTypeChanged}/>
              </td>
              <td className="text-middle">
                <form onSubmit={this.onSubmit} className="search-box">
                  <button className="search-box-submit button-clean">
                    <i className="icon-search"></i>
                  </button>
                  <input onChange={this.search}
                         value={this.props.query}
                         ref="input"
                         className="search-box-input"
                         type="search"
                         placeholder="Search"/>
                </form>
              </td>
              <td className="thin text-middle">
                <button onClick={this.deleteProjects} className="button-red"
                        disabled={!isSomethingSelected}>Delete
                </button>
              </td>
            </tr>
            </tbody>
          </table>
          {this.renderGhostsDescription()}
        </div>
    );
  }
});
