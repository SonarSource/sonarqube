import ModalForm from '../../components/common/modal-form';
import { deletePermissionTemplate } from '../../api/permissions';
import Template from './templates/permission-templates-delete.hbs';

export default ModalForm.extend({
  template: Template,

  onFormSubmit: function () {
    ModalForm.prototype.onFormSubmit.apply(this, arguments);
    this.sendRequest();
  },

  sendRequest: function () {
    var that = this;
    return deletePermissionTemplate({
      data: { templateId: this.model.id },
      statusCode: {
        // do not show global error
        400: null
      }
    }).done(function () {
      that.options.refresh();
      that.destroy();
    }).fail(function (jqXHR) {
      that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
    });
  }
});
