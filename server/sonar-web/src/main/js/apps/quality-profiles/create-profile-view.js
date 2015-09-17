import $ from 'jquery';
import _ from 'underscore';
import ModalFormView from 'components/common/modal-form';
import uploader from 'components/common/file-upload';
import Profile from './profile';
import './templates';

export default ModalFormView.extend({
  template: Templates['quality-profiles-create-profile'],

  events: function () {
    return _.extend(ModalFormView.prototype.events.apply(this, arguments), {
      'change #create-profile-language': 'onLanguageChange'
    });
  },

  onFormSubmit: function (e) {
    ModalFormView.prototype.onFormSubmit.apply(this, arguments);
    this.sendRequest(e);
  },

  onRender: function () {
    ModalFormView.prototype.onRender.apply(this, arguments);
    this.$('select').select2({
      width: '250px',
      minimumResultsForSearch: 50
    });
    this.onLanguageChange();
  },

  onLanguageChange: function () {
    var that = this;
    var language = this.$('#create-profile-language').val();
    var importers = this.getImportersForLanguages(language);
    this.$('.js-importer').each(function () {
      that.emptyInput($(this));
      $(this).addClass('hidden');
    });
    importers.forEach(function (importer) {
      that.$('.js-importer[data-key="' + importer.key + '"]').removeClass('hidden');
    });
  },

  emptyInput: function (e) {
    e.wrap('<form>').closest('form').get(0).reset();
    e.unwrap();
  },

  sendRequest: function (e) {
    var that = this;
    uploader({ form: $(e.currentTarget) }).done(function (r) {
      if (_.isArray(r.errors) || _.isArray(r.warnings)) {
        that.showErrors(r.errors, r.warnings);
      } else {
        that.addProfile(r.profile);
        that.destroy();
      }
    });
  },

  addProfile: function (profileData) {
    var profile = new Profile(profileData);
    this.collection.add([profile]);
    profile.trigger('select', profile);
  },

  getImportersForLanguages: function (language) {
    if (language != null) {
      return this.options.importers.filter(function (importer) {
        return importer.languages.indexOf(language) !== -1;
      });
    } else {
      return [];
    }
  },

  serializeData: function () {
    return _.extend(ModalFormView.prototype.serializeData.apply(this, arguments), {
      languages: this.options.languages,
      importers: this.options.importers
    });
  }
});


