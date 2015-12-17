import _ from 'underscore';
import Modal from '../../components/common/modals';
import '../../components/common/select-list';
import Template from './templates/permission-templates-groups.hbs';

function getSearchUrl (permission, permissionTemplate) {
  return baseUrl + '/api/permissions/template_groups?ps=100&permission=' + permission.key +
      '&templateId=' + permissionTemplate.id;
}

export default Modal.extend({
  template: Template,

  onRender: function () {
    Modal.prototype.onRender.apply(this, arguments);
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
        permission: this.options.permission.key,
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
    Modal.prototype.onDestroy.apply(this, arguments);
  },

  serializeData: function () {
    return _.extend(Modal.prototype.serializeData.apply(this, arguments), {
      permissionName: this.options.permission.name,
      permissionTemplateName: this.options.permissionTemplate.name
    });
  }
});
