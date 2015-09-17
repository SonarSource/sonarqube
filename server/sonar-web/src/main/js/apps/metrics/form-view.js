import $ from 'jquery';
import _ from 'underscore';
import ModalForm from 'components/common/modal-form';
import './templates';

export default ModalForm.extend({
  template: Templates['metrics-form'],

  onRender: function () {
    var that = this;
    this._super();
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
    this.$('#create-metric-domain').select2({
      width: '250px',
      createSearchChoice: function (term) {
        return { id: term, text: '+' + term };
      },
      createSearchChoicePosition: 'top',
      initSelection: function (element, callback) {
        var value = $(element).val();
        callback({ id: value, text: value });
      },
      query: function (options) {
        var items = that.options.domains.filter(function (d) {
              return d.toLowerCase().indexOf(options.term.toLowerCase()) !== -1;
            }),
            results = items.map(function (item) {
              return { id: item, text: item };
            });
        options.callback({ results: results, more: false });
      }
    }).select2('val', this.model && this.model.get('domain'));
    this.$('#create-metric-type').select2({ width: '250px' });
  },

  onDestroy: function () {
    this._super();
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  onFormSubmit: function (e) {
    this._super(e);
    this.sendRequest();
  },

  serializeData: function () {
    return _.extend(this._super(), {
      domains: this.options.domains,
      types: this.options.types
    });
  }

});


