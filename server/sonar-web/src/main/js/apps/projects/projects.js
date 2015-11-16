import classNames from 'classnames';
import React from 'react';
import { getComponentUrl } from '../../helpers/urls';
import Checkbox from '../../components/shared/checkbox';
import QualifierIcon from '../../components/shared/qualifier-icon';

export default React.createClass({
  propTypes: {
    projects: React.PropTypes.array.isRequired,
    selection: React.PropTypes.array.isRequired,
    refresh: React.PropTypes.func.isRequired
  },

  onProjectCheck(project, checked) {
    if (checked) {
      this.props.onProjectSelected(project);
    } else {
      this.props.onProjectDeselected(project);
    }
  },

  isProjectSelected(project) {
    return this.props.selection.indexOf(project.id) !== -1;
  },

  renderProject(project) {
    return (
        <tr key={project.id}>
          <td className="thin">
            <Checkbox onCheck={this.onProjectCheck.bind(this, project)}
                      initiallyChecked={this.isProjectSelected(project)}/>
          </td>
          <td className="thin">
            <QualifierIcon qualifier={project.qualifier}/>
          </td>
          <td className="nowrap">
            <a href={getComponentUrl(project.key)}>{project.name}</a>
          </td>
          <td className="nowrap">
            <span className="note">{project.key}</span>
          </td>
        </tr>
    );
  },

  render() {
    let className = classNames('data', 'zebra', { 'new-loading': !this.props.ready });
    return (
        <table className={className}>
          <tbody>{this.props.projects.map(this.renderProject)}</tbody>
        </table>
    );
  }
});
