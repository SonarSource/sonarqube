import Modal from 'components/common/modals';
import 'components/common/select-list';
import './templates';

function getSearchUrl (permission, project) {
  var url = baseUrl + '/api/permissions/groups?ps=100&permission=' + permission;
  if (project) {
    url = url + '&projectId=' + project;
  }
  return url;
}

function getExtra (permission, project) {
  var extra = { permission: permission };
  if (project) {
    extra.projectId = project;
  }
  return extra;
}

export default Modal.extend({
  template: Templates['global-permissions-groups'],

  onRender: function () {
    this._super();
    new window.SelectList({
      el: this.$('#global-permissions-groups'),
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
      extra: getExtra(this.options.permission, this.options.project),
      selectParameter: 'groupName',
      selectParameterValue: 'name',
      parse: function (r) {
        this.more = false;
        return r.groups;
      }
    });
  },

  onDestroy: function () {
    this.options.refresh();
    this._super();
  }
});


