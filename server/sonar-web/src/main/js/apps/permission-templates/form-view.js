import ModalForm from '../../components/common/modal-form';
import Template from './templates/permission-templates-form.hbs';

export default ModalForm.extend({
  template: Template,

  onRender: function () {
    this._super();
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
    this.$('#create-custom-measure-metric').select2({
      width: '250px',
      minimumResultsForSearch: 20
    });
  },

  onDestroy: function () {
    this._super();
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  onFormSubmit: function () {
    ModalForm.prototype.onFormSubmit.apply(this, arguments);
    this.sendRequest();
  }
});
