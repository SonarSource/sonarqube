import Modal from '../../components/common/modals';
import '../../components/common/select-list';
import './templates';

export default Modal.extend({
  template: Templates['permission-templates-users'],

  onRender: function () {
    this._super();
    var searchUrl = baseUrl + '/api/permissions/template_users?ps=100&permission=' + this.options.permission +
        '&templateId=' + this.options.permissionTemplate.id;
    new window.SelectList({
      el: this.$('#permission-templates-users'),
      width: '100%',
      readOnly: false,
      focusSearch: false,
      format: function (item) {
        return item.name + '<br><span class="note">' + item.login + '</span>';
      },
      queryParam: 'q',
      searchUrl: searchUrl,
      selectUrl: baseUrl + '/api/permissions/add_user_to_template',
      deselectUrl: baseUrl + '/api/permissions/remove_user_from_template',
      extra: {
        permission: this.options.permission,
        templateId: this.options.permissionTemplate.id
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
      permissionTemplateName: this.options.permissionTemplate.name
    });
  }
});
