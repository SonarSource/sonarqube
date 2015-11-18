import $ from 'jquery';
import _ from 'underscore';
import ModalFormView from '../../components/common/modal-form';
import Template from './templates/quality-profiles-restore-built-in-profiles.hbs';
import TemplateSuccess from './templates/quality-profiles-restore-built-in-profiles-success.hbs';

export default ModalFormView.extend({
  template: Template,
  successTemplate: TemplateSuccess,

  getTemplate: function () {
    return this.selectedLanguage ? this.successTemplate : this.template;
  },

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
        options = { language: lang };
    this.selectedLanguage = _.findWhere(this.options.languages, { key: lang }).name;
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
      that.render();
    }).fail(function (jqXHR) {
      that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
      that.enableForm();
    });
  },

  serializeData: function () {
    return _.extend(ModalFormView.prototype.serializeData.apply(this, arguments), {
      languages: this.options.languages,
      selectedLanguage: this.selectedLanguage
    });
  }
});


