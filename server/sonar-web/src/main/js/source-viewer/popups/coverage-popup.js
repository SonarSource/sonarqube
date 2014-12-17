define([
  'backbone.marionette',
  'templates/source-viewer',
  'common/popup'
], function (Marionette, Templates, Popup) {

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
      var key = $(e.currentTarget).data('key'),
          url = baseUrl + '/component/index?id=' + encodeURIComponent(key),
          windowParams = 'resizable=1,scrollbars=1,status=1';
      window.open(url, key, windowParams);
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
      return {
        testFiles: testFiles,
        row: this.options.row
      };
    }
  });
});
