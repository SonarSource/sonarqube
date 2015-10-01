import ModalForm from 'components/common/modal-form';
import './templates';

export default ModalForm.extend({
  template: Templates['projects-delete'],

  onFormSubmit: function (e) {
    this._super(e);
    this.options.deleteProjects();
    this.destroy();
  }
});


