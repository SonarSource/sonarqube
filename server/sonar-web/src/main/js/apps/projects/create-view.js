import ModalForm from '../../components/common/modal-form';
import { createProject } from '../../api/components';
import Template from './templates/projects-create-form.hbs';


export default ModalForm.extend({
  template: Template,

  onRender: function () {
    ModalForm.prototype.onRender.apply(this, arguments);
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
  },

  onDestroy: function () {
    ModalForm.prototype.onDestroy.apply(this, arguments);
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  onFormSubmit: function () {
    ModalForm.prototype.onFormSubmit.apply(this, arguments);
    this.sendRequest();
  },

  sendRequest: function () {
    let data = {
      name: this.$('#create-project-name').val(),
      branch: this.$('#create-project-branch').val(),
      key: this.$('#create-project-key').val()
    };
    this.disableForm();
    return createProject(data)
        .then(() => {
          if (this.options.refresh) {
            this.options.refresh();
          }
          this.destroy();
        })
        .catch(error => {
          this.enableForm();
          if (error.response.status === 400) {
            error.response.json().then(obj => this.showErrors([{ msg: obj.err_msg }]));
          }
        });
  }
});
