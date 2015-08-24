define([
  'components/common/modals',
  'components/common/select-list',
  './templates'
], function (Modal) {

  return Modal.extend({
    template: Templates['global-permissions-users'],

    onRender: function () {
      Modal.prototype.onRender.apply(this, arguments);
      new window.SelectList({
        el: this.$('#global-permissions-users'),
        width: '100%',
        readOnly: false,
        focusSearch: false,
        format: function (item) {
          return item.name + '<br><span class="note">' + item.login + '</span>';
        },
        queryParam: 'q',
        searchUrl: baseUrl + '/api/permissions/users?ps=100&permission=' + this.options.permission,
        selectUrl: baseUrl + '/api/permissions/add_user',
        deselectUrl: baseUrl + '/api/permissions/remove_user',
        extra: {
          permission: this.options.permission
        },
        selectParameter: 'login',
        selectParameterValue: 'login',
        parse: function (r) {
          this.more = false;
          return r.users;
        }
      });
    },

    onDestroy: function () {
      this.options.refresh();
      Modal.prototype.onDestroy.apply(this, arguments);
    }
  });

});
