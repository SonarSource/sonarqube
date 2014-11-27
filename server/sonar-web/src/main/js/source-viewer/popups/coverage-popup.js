define([
  'backbone.marionette',
  'templates/source-viewer',
  'common/popup',
  'component-viewer/utils'
], function (Marionette, Templates, Popup, utils) {

  var $ = jQuery;

  return Popup.extend({
    template: Templates['source-viewer-coverage-popup'],

    events: {
      'click a[data-key]': 'goToFile'
    },

    onRender: function () {
      Popup.prototype.onRender.apply(this, arguments);
      this.$('.bubble-popup-container').isolatedScroll();
    },

    goToFile: function (e) {
      // TODO Implement this
      var el = $(e.currentTarget),
          key = el.data('key'),
          method = el.data('method'),
          files = this.model.get('files');
    },

    serializeData: function () {
      var files = this.model.get('files'),
          tests = _.groupBy(this.model.get('tests'), '_ref'),
          testFiles = _.map(tests, function (testSet, fileRef) {
            return {
              file: files[fileRef],
              tests: testSet
            };
          });
      return { testFiles: testFiles };
    }
  });
});
