import FormView from './form-view';
import { createPermissionTemplate } from '../../api/permissions';

export default FormView.extend({
  sendRequest: function () {
    var that = this;
    this.disableForm();
    return createPermissionTemplate({
      data: {
        name: this.$('#permission-template-name').val(),
        description: this.$('#permission-template-description').val(),
        projectKeyPattern: this.$('#permission-template-project-key-pattern').val()
      },
      statusCode: {
        // do not show global error
        400: null
      }
    }).done(function () {
      that.options.refresh();
      that.destroy();
    }).fail(function (jqXHR) {
      that.enableForm();
      that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
    });
  }
});
