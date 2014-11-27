define([
  'backbone.marionette',
  'templates/component-viewer',
  'common/popup',
  'component-viewer/utils'
], function (Marionette, Templates, Popup, utils) {

  var $ = jQuery;

  return Popup.extend({
    template: Templates['source-viewer-duplication-popup'],

    events: {
      'click a[data-key]': 'goToFile'
    },

    goToFile: function (e) {
      var key = $(e.currentTarget).data('key'),
          line = $(e.currentTarget).data('line'),
          files = this.options.main.source.get('duplicationFiles'),
          options = this.collection.map(function (item) {
            var file = files[item.get('_ref')],
                x = utils.splitLongName(file.name);
            return {
              key: file.key,
              name: x.name,
              subname: x.dir,
              component: {
                projectName: file.projectName,
                subProjectName: file.subProjectName
              },
              active: file.key === key
            };
          });
      return _.uniq(options, function (item) {
        return item.key;
      });
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
