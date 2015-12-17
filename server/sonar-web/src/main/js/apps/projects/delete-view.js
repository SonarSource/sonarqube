import ModalForm from '../../components/common/modal-form';
import Template from './templates/projects-delete.hbs';

export default ModalForm.extend({
  template: Template,

  onFormSubmit: function () {
    ModalForm.prototype.onFormSubmit.apply(this, arguments);
    this.options.deleteProjects();
    this.destroy();
  }
});


