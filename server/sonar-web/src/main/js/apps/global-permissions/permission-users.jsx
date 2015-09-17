import React from 'react';
import PermissionsUsersGroupsMixin from './permission-users-groups-mixin';
import UsersView from './users-view';

export default React.createClass({
  mixins: [PermissionsUsersGroupsMixin],

  renderUpdateLink() {
    return (
        <a onClick={this.updateUsers}
           className="icon-bullet-list"
           title="Update Users"
           data-toggle="tooltip"
           href="#"></a>
    );
  },

  renderItem(item) {
    return item.name;
  },

  renderTitle() {
    return 'Users';
  },

  updateUsers(e) {
    e.preventDefault();
    new UsersView({
      permission: this.props.permission.key,
      project: this.props.project,
      refresh: this.props.refresh
    }).render();
  }
});
