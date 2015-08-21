define([
  'components/common/modals',
  'react',
  'components/select-list/main',
  '../../api/permissions',
  './templates'
], function (Modal, React, SelectList, Permissions) {

  return Modal.extend({
    template: Templates['global-permissions-users'],

    onRender: function () {
      var that = this;
      this._super();
      var props = {
        loadItems: function (options, callback) {
          var data = { permission: that.options.permission, p: options.page, ps: 100 };
          options.query ? _.extend(data, { q: options.query }) : _.extend(data, { selected: options.selection });
          Permissions.getUsers(data).done(function (r) {
            var paging = _.defaults({}, r.paging, { total: 0, pageIndex: 1 });
            callback(r.users, paging);
          });
        },
        renderItem: function (user) {
          return user.name + '<br><span class="note">' + user.login + '</span>';
        },
        getItemKey: function (user) {
          return user.login;
        },
        selectItem: function (user, callback) {
          Permissions.grantToUser(that.options.permission, user.login).done(callback);
        },
        deselectItem: function (user, callback) {
          Permissions.revokeFromUser(that.options.permission, user.login).done(callback);
        }
      };
      React.render(
          React.createElement(SelectList, props),
          this.$('#global-permissions-users')[0]
      );
    },

    onDestroy: function () {
      this.options.refresh();
      this._super();
    }
  });

});
