import $ from 'jquery';
import _ from 'underscore';
import ModalForm from 'components/common/modal-form';
import './templates';

export default ModalForm.extend({
  template: Templates['users-form'],

  events: function () {
    return _.extend(this._super(), {
      'click #create-user-add-scm-account': 'onAddScmAccountClick'
    });
  },

  onRender: function () {
    this._super();
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
  },

  onDestroy: function () {
    this._super();
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  onFormSubmit: function (e) {
    this._super(e);
    this.sendRequest();
  },

  onAddScmAccountClick: function (e) {
    e.preventDefault();
    this.addScmAccount();
  },

  getScmAccounts: function () {
    var scmAccounts = this.$('[name="scmAccounts"]').map(function () {
      return $(this).val();
    }).toArray();
    return scmAccounts.filter(function (value) {
      return !!value;
    });
  },

  addScmAccount: function () {
    var fields = this.$('[name="scmAccounts"]');
    fields.first().clone().val('').insertAfter(fields.last());
  }
});


