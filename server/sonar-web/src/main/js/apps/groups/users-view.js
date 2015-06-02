define([
  'components/common/modals',
  'components/common/select-list',
  './templates'
], function (Modal) {

  return Modal.extend({
    template: Templates['groups-users'],

    onRender: function () {
      Modal.prototype.onRender.apply(this, arguments);
      new window.SelectList({
        el: this.$('#groups-users'),
        width: '100%',
        readOnly: false,
        focusSearch: false,
        format: function (item) {
          return item.name + '<br><span class="note">' + item.login + '</span>';
        },
        queryParam: 'q',
        searchUrl: baseUrl + '/api/usergroups/users?ps=100&id=' + this.model.id,
        selectUrl: baseUrl + '/api/usergroups/add_user',
        deselectUrl: baseUrl + '/api/usergroups/remove_user',
        extra: {
          groupId: this.model.id
        },
        selectParameter: 'userLogin',
        selectParameterValue: 'login',
        parse: function (r) {
          this.more = false;
          return r.users;
        }
      });
    }
  });

});
