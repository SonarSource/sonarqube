import _ from 'underscore';
import Backbone from 'backbone';
import ModalForm from 'components/common/modal-form';
import Gate from './gate';
import './templates';

export default ModalForm.extend({
  template: Templates['quality-gate-form'],

  onFormSubmit: function () {
    ModalForm.prototype.onFormSubmit.apply(this, arguments);
    this.disableForm();
    this.prepareRequest();
  },

  sendRequest: function (options) {
    var that = this,
        opts = _.defaults(options || {}, {
          type: 'POST',
          statusCode: {
            // do not show global error
            400: null
          }
        });
    return Backbone.ajax(opts)
        .done(function () {
          that.destroy();
        }).fail(function (jqXHR) {
          that.enableForm();
          that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
        });
  },

  addGate: function (attrs) {
    var gate = new Gate(attrs);
    this.collection.add(gate, { merge: true });
    return gate;
  },

  serializeData: function () {
    return _.extend(ModalForm.prototype.serializeData.apply(this, arguments), {
      method: this.method
    });
  }
});


