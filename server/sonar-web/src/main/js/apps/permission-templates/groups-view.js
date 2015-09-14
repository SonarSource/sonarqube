import Modal from '../../components/common/modals';
import '../../components/common/select-list';
import './templates';

function getSearchUrl (permission, permissionTemplate) {
  return baseUrl + '/api/permissions/template_groups?ps=100&permission=' + permission +
      '&templateId=' + permissionTemplate.id;
}

export default Modal.extend({
  template: Templates['permission-templates-groups'],

  onRender: function () {
    this._super();
    new window.SelectList({
      el: this.$('#permission-templates-groups'),
      width: '100%',
      readOnly: false,
      focusSearch: false,
      format: function (item) {
        return item.name;
      },
      queryParam: 'q',
      searchUrl: getSearchUrl(this.options.permission, this.options.permissionTemplate),
      selectUrl: baseUrl + '/api/permissions/add_group_to_template',
      deselectUrl: baseUrl + '/api/permissions/remove_group_from_template',
      extra: {
        permission: this.options.permission,
        templateId: this.options.permissionTemplate.id
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
      permissionTemplateName: this.options.permissionTemplate.name
    });
  }
});
