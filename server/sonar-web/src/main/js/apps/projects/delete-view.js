import ModalForm from '../../components/common/modal-form';
import Template from './templates/projects-delete.hbs';

export default ModalForm.extend({
  template: Template,

  onFormSubmit: function (e) {
    this._super(e);
    this.options.deleteProjects();
    this.destroy();
  }
});


