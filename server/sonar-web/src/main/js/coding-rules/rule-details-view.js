define([
    'backbone.marionette',
    'templates/coding-rules'
], function (Marionette, Templates) {

  return Marionette.ItemView.extend({
    template: Templates['coding-rules-rule-details'],

    modelEvents: {
      'change': 'render'
    },

    initialize: function () {
      this.bindShortcuts();
    },

    onClose: function () {
      this.unbindShortcuts();
    },

    bindShortcuts: function () {
      var that = this;
      key('up', 'details', function () {
        that.options.app.controller.selectPrev();
        that.options.app.controller.showDetailsForSelected();
        return false;
      });
      key('down', 'details', function () {
        that.options.app.controller.selectNext();
        that.options.app.controller.showDetailsForSelected();
        return false;
      });
      key('left', 'details', function () {
        that.options.app.controller.hideDetails();
        return false;
      });
    },

    unbindShortcuts: function () {
      key.deleteScope('details');
    },

    serializeData: function () {
      var isManual = (this.options.app.manualRepository().key === this.model.get('repo'));

      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        language: this.options.app.languages[this.model.get('lang')],
        repository: _.findWhere(this.options.app.repositories, { key: this.model.get('repo') }).name,
        isManual: isManual,
        subCharacteristic: this.options.app.getSubCharacteristicName(this.model.get('debtSubChar')),
        allTags: _.union(this.model.get('sysTags'), this.model.get('tags'))
      });
    }
  });

});
