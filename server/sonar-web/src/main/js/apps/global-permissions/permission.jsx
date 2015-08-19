import React from 'react';
import PermissionUsers from './permission-users';
import PermissionGroups from './permission-groups';

let $ = jQuery;

// Maximum number of displayed groups
const MAX_ITEMS = 3;

export default React.createClass({
  propTypes: {
    permission: React.PropTypes.object.isRequired
  },

  getInitialState() {
    return {};
  },

  componentDidMount() {
    this.requestUsers();
    this.requestGroups();
  },

  requestUsers() {
    const url = `${window.baseUrl}/api/permissions/users`;
    const data = { permission: this.props.permission.key, ps: MAX_ITEMS };
    $.get(url, data).done(r => this.setState({ users: r.users, totalUsers: r.paging && r.paging.total }));
  },

  requestGroups() {
    const url = `${window.baseUrl}/api/permissions/groups`;
    const data = { permission: this.props.permission.key, ps: MAX_ITEMS };
    $.get(url, data).done(r => this.setState({ groups: r.groups, totalGroups: r.paging && r.paging.total }));
  },

  render() {
    return (
        <li className="panel panel-vertical" data-id={this.props.permission.key}>
          <h3>{this.props.permission.name}</h3>
          <p className="spacer-top" dangerouslySetInnerHTML={{ __html: this.props.permission.description }}/>
          <ul className="list-inline spacer-top">
            <PermissionUsers permission={this.props.permission}
                             max={MAX_ITEMS}
                             items={this.state.users}
                             total={this.state.totalUsers || this.props.permission.usersCount}
                             refresh={this.requestUsers}/>
            <PermissionGroups permission={this.props.permission}
                              max={MAX_ITEMS}
                              items={this.state.groups}
                              total={this.state.totalGroups || this.props.permission.groupsCount}
                              refresh={this.requestGroups}/>
          </ul>
        </li>
    );
  }
});
