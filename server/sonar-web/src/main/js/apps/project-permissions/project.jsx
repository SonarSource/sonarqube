import React from 'react';
import UsersView from './users-view';
import GroupsView from './groups-view';
import ApplyTemplateView from './apply-template-view';
import {getProjectUrl} from '../../helpers/Url';

export default React.createClass({
  propTypes: {
    project: React.PropTypes.object.isRequired,
    permissionTemplates: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
    refresh: React.PropTypes.func.isRequired
  },

  showGroups(permission, e) {
    e.preventDefault();
    new GroupsView({
      permission: permission,
      project: this.props.project.id,
      projectName: this.props.project.name,
      refresh: this.props.refresh
    }).render();
  },

  showUsers(permission, e) {
    e.preventDefault();
    new UsersView({
      permission: permission,
      project: this.props.project.id,
      projectName: this.props.project.name,
      refresh: this.props.refresh
    }).render();
  },

  applyTemplate(e) {
    e.preventDefault();
    new ApplyTemplateView({
      permissionTemplates: this.props.permissionTemplates,
      project: this.props.project,
      refresh: this.props.refresh
    }).render();
  },

  render() {
    let permissions = this.props.project.permissions.map(p => {
      return (
          <td key={p.key}>
            <table>
              <tr>
                <td className="spacer-right">Users</td>
                <td className="spacer-left bordered-left">{p.usersCount}</td>
                <td className="spacer-left">
                  <a onClick={this.showUsers.bind(this, p.key)} className="icon-bullet-list" title="Update Users"
                     data-toggle="tooltip" href="#"></a>
                </td>
              </tr>
              <tr>
                <td className="spacer-right">Groups</td>
                <td className="spacer-left bordered-left">{p.groupsCount}</td>
                <td className="spacer-left">
                  <a onClick={this.showGroups.bind(this, p.key)} className="icon-bullet-list" title="Update Users"
                     data-toggle="tooltip" href="#"></a>
                </td>
              </tr>
            </table>
          </td>
      );
    });
    return (
        <tr>
          <td>
            <strong>
              <a href={getProjectUrl(this.props.project.key)}>{this.props.project.name}</a>
            </strong>
          </td>
          {permissions}
          <td className="thin nowrap text-right">
            <button onClick={this.applyTemplate} className="js-apply-template">Apply Template</button>
          </td>
        </tr>
    );
  }
});
