import $ from 'jquery';
import _ from 'underscore';
import ModalFormView from '../../components/common/modal-form';
import Template from './templates/quality-profiles-restore-built-in-profiles.hbs';

export default ModalFormView.extend({
  template: Template,

  onFormSubmit: function () {
    ModalFormView.prototype.onFormSubmit.apply(this, arguments);
    this.disableForm();
    this.sendRequest();
  },

  onRender: function () {
    ModalFormView.prototype.onRender.apply(this, arguments);
    this.$('select').select2({
      width: '250px',
      minimumResultsForSearch: 50
    });
  },

  sendRequest: function () {
    var that = this,
        url = baseUrl + '/api/qualityprofiles/restore_built_in',
        lang = this.$('#restore-built-in-profiles-language').val(),
        langName = _.findWhere(this.options.languages, { key: lang }).name,
        options = { language: lang };
    return $.ajax({
      type: 'POST',
      url: url,
      data: options,
      statusCode: {
        // do not show global error
        400: null
      }
    }).done(function () {
      that.collection.fetch({ reset: true });
      that.collection.trigger('destroy');
      that.$('#restore-built-in-profiles-form-success')
          .text(window.tp('quality_profiles.restore_built_in_profiles_success_message', langName))
          .removeClass('hidden');
      that.enableForm();
    }).fail(function (jqXHR) {
      that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
      that.enableForm();
    });
  },

  serializeData: function () {
    return _.extend(ModalFormView.prototype.serializeData.apply(this, arguments), {
      languages: this.options.languages
    });
  }
});


