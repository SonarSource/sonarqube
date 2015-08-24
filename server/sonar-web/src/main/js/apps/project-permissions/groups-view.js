define([
  'components/common/modals',
  'components/common/select-list',
  './templates'
], function (Modal) {

  return Modal.extend({
    template: Templates['project-permissions-groups'],

    onRender: function () {
      Modal.prototype.onRender.apply(this, arguments);
      new window.SelectList({
        el: this.$('#project-permissions-groups'),
        width: '100%',
        readOnly: false,
        focusSearch: false,
        format: function (item) {
          return item.name;
        },
        queryParam: 'q',
        searchUrl: baseUrl + '/api/permissions/groups?ps=100&permission=' + this.options.permission + '&projectId=' + this.options.project,
        selectUrl: baseUrl + '/api/permissions/add_group',
        deselectUrl: baseUrl + '/api/permissions/remove_group',
        extra: {
          permission: this.options.permission,
          projectId: this.options.project
        },
        selectParameter: 'groupName',
        selectParameterValue: 'name',
        parse: function (r) {
          this.more = false;
          return r.groups;
        }
      });
    },

    onDestroy: function () {
      if (this.options.refresh) {
        this.options.refresh();
      }
      Modal.prototype.onDestroy.apply(this, arguments);
    },

    serializeData: function () {
      return _.extend(Modal.prototype.serializeData.apply(this, arguments), {
        projectName: this.options.projectName
      })
    }
  });

});
