define([
  'components/common/modals',
  'components/common/select-list',
  './templates'
], function (Modal) {

  return Modal.extend({
    template: Templates['users-groups'],
    itemTemplate: Templates['users-group'],

    onRender: function () {
      Modal.prototype.onRender.apply(this, arguments);
      new window.SelectList({
        el: this.$('#users-groups'),
        width: '100%',
        readOnly: false,
        focusSearch: false,
        format: function (item) {
          return item.name + '<br><span class="note">' + item.description + '</span>';
        },
        searchUrl: baseUrl + '/api/users/groups?ps=100&login=' + this.model.id,
        selectUrl: baseUrl + '/api/usergroups/add_user',
        deselectUrl: baseUrl + '/api/usergroups/remove_user',
        extra: {
          userLogin: this.model.id
        },
        selectParameter: 'groupId',
        selectParameterValue: 'id',
        parse: function (r) {
          this.more = false;
          return r.groups;
        }
      });
    }
  });

});
