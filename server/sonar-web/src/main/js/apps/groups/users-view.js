define([
  'components/common/modals',
  'components/common/select-list',
  './templates'
], function (Modal) {

  return Modal.extend({
    template: Templates['groups-users'],

    onRender: function () {
      Modal.prototype.onRender.apply(this, arguments);
      //noinspection Eslint
      new window.SelectList({
        el: this.$('#groups-users'),
        width: '100%',
        readOnly: false,
        focusSearch: false,
        format: function (item) {
          return item.name + '<br><span class="note">' + item.login + '</span>';
        },
        queryParam: 'q',
        searchUrl: window.baseUrl + '/api/usergroups/users?ps=100&id=' + this.model.id,
        selectUrl: window.baseUrl + '/api/usergroups/add_user',
        deselectUrl: window.baseUrl + '/api/usergroups/remove_user',
        extra: {
          id: this.model.id
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
      this.model.collection.refresh();
      Modal.prototype.onDestroy.apply(this, arguments);
    }
  });

});
