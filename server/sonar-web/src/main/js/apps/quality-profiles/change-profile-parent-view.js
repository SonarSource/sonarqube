import $ from 'jquery';
import _ from 'underscore';
import Marionette from 'backbone.marionette';
import ModalFormView from 'components/common/modal-form';
import './templates';

export default ModalFormView.extend({
  template: Templates['quality-profiles-change-profile-parent'],

  onRender: function () {
    ModalFormView.prototype.onRender.apply(this, arguments);
    this.$('select').select2({
      width: '250px',
      minimumResultsForSearch: 50
    });
  },

  onFormSubmit: function () {
    ModalFormView.prototype.onFormSubmit.apply(this, arguments);
    this.disableForm();
    this.sendRequest();
  },

  sendRequest: function () {
    var that = this,
        url = baseUrl + '/api/qualityprofiles/change_parent',
        parent = this.$('#change-profile-parent').val(),
        options = {
          profileKey: this.model.get('key'),
          parentKey: parent
        };
    return $.ajax({
      type: 'POST',
      url: url,
      data: options,
      statusCode: {
        // do not show global error
        400: null
      }
    }).done(function () {
      that.model.collection.fetch();
      that.model.trigger('select', that.model);
      that.destroy();
    }).fail(function (jqXHR) {
      that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
      that.enableForm();
    });
  },

  serializeData: function () {
    var that = this,
        profilesData = this.model.collection.toJSON(),
        profiles = _.filter(profilesData, function (profile) {
          return profile.language === that.model.get('language') && profile.key !== that.model.id;
        });
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      profiles: profiles
    });
  }
});


