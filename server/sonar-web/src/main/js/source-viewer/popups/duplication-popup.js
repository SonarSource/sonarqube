define([
  'backbone.marionette',
  'templates/component-viewer',
  'common/popup'
], function (Marionette, Templates, Popup) {

  var $ = jQuery;

  return Popup.extend({
    template: Templates['source-viewer-duplication-popup'],

    events: {
      'click a[data-key]': 'goToFile'
    },

    goToFile: function (e) {
      var key = $(e.currentTarget).data('key'),
          line = $(e.currentTarget).data('line'),
          url = baseUrl + '/component/index?id=' + encodeURIComponent(key) + (line ? ('&line=' + line) : ''),
          windowParams = 'resizable=1,scrollbars=1,status=1';
      window.open(url, key, windowParams);
    },

    serializeData: function () {
      var duplications, files, groupedBlocks;
      files = this.model.get('duplicationFiles');
      groupedBlocks = _.groupBy(this.collection.toJSON(), '_ref');
      duplications = _.map(groupedBlocks, function (blocks, fileRef) {
        return {
          blocks: blocks,
          file: files[fileRef]
        };
      });
      duplications = _.sortBy(duplications, (function (_this) {
        return function (d) {
          var a, b, c;
          a = d.file.projectName !== _this.model.get('projectName');
          b = d.file.subProjectName !== _this.model.get('subProjectName');
          c = d.file.key !== _this.model.get('key');
          return '' + a + b + c;
        };
      })(this));
      return {
        component: this.model.toJSON(),
        duplications: duplications
      };
    }
  });
});
