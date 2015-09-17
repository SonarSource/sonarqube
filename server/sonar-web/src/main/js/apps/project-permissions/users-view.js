import _ from 'underscore';
import Modal from 'components/common/modals';
import 'components/common/select-list';
import './templates';

export default Modal.extend({
  template: Templates['project-permissions-users'],

  onRender: function () {
    this._super();
    var searchUrl = baseUrl + '/api/permissions/users?ps=100&permission=' + this.options.permission +
        '&projectId=' + this.options.project;
    new window.SelectList({
      el: this.$('#project-permissions-users'),
      width: '100%',
      readOnly: false,
      focusSearch: false,
      format: function (item) {
        return item.name + '<br><span class="note">' + item.login + '</span>';
      },
      queryParam: 'q',
      searchUrl: searchUrl,
      selectUrl: baseUrl + '/api/permissions/add_user',
      deselectUrl: baseUrl + '/api/permissions/remove_user',
      extra: {
        permission: this.options.permission,
        projectId: this.options.project
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


