import _ from 'underscore';
import Modal from 'components/common/modals';
import 'components/common/select-list';
import './templates';

function getSearchUrl (permission, project) {
  return baseUrl + '/api/permissions/groups?ps=100&permission=' + permission + '&projectId=' + project;
}

export default Modal.extend({
  template: Templates['project-permissions-groups'],

  onRender: function () {
    this._super();
    new window.SelectList({
      el: this.$('#project-permissions-groups'),
      width: '100%',
      readOnly: false,
      focusSearch: false,
      format: function (item) {
        return item.name;
      },
      queryParam: 'q',
      searchUrl: getSearchUrl(this.options.permission, this.options.project),
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
    this._super();
  },

  serializeData: function () {
    return _.extend(Modal.prototype.serializeData.apply(this, arguments), {
      projectName: this.options.projectName
    });
  }
});


