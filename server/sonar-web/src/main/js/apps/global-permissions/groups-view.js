define([
  'components/common/modals',
  'react',
  'components/select-list/main',
  '../../api/permissions',
  './templates'
], function (Modal, React, SelectList, Permissions) {

  return Modal.extend({
    template: Templates['global-permissions-groups'],

    onRender: function () {
      var that = this;
      this._super();
      var props = {
        loadItems: function (options, callback) {
          var _data = { permission: that.options.permission, p: options.page, ps: 100 };
          options.query ? _.extend(_data, { q: options.query }) : _.extend(_data, { selected: options.selection });
          Permissions.getGroups(_data).done(function (r) {
            var paging = _.defaults({}, r.paging, { total: 0, pageIndex: 1 });
            callback(r.groups, paging);
          });
        },
        renderItem: function (group) {
          return group.name;
        },
        getItemKey: function (group) {
          return group.name;
        },
        selectItem: function (group, callback) {
          Permissions.grantToGroup(that.options.permission, group.name).done(callback);
        },
        deselectItem: function (group, callback) {
          Permissions.revokeFromGroup(that.options.permission, group.name).done(callback);
        }
      };
      React.render(
          React.createElement(SelectList, props),
          this.$('#global-permissions-groups')[0]
      );
    },

    onDestroy: function () {
      this.options.refresh();
      this._super();
    }
  });

});
