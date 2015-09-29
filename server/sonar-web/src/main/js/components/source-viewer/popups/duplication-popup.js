import $ from 'jquery';
import _ from 'underscore';
import Popup from 'components/common/popup';
import Workspace from 'components/workspace/main';
import '../templates';

export default Popup.extend({
  template: Templates['source-viewer-duplication-popup'],

  events: {
    'click a[data-uuid]': 'goToFile'
  },

  goToFile: function (e) {
    e.stopPropagation();
    var uuid = $(e.currentTarget).data('uuid'),
        line = $(e.currentTarget).data('line');
    var RealWorkspace = Workspace.openComponent ? Workspace : require('components/workspace/main');
    RealWorkspace.openComponent({ uuid: uuid, line: line });
  },

  serializeData: function () {
    var that = this,
        files = this.model.get('duplicationFiles'),
        groupedBlocks = _.groupBy(this.collection.toJSON(), '_ref'),
        duplications = _.map(groupedBlocks, function (blocks, fileRef) {
          return {
            blocks: blocks,
            file: files[fileRef]
          };
        });
    duplications = _.sortBy(duplications, function (d) {
      var a = d.file.projectName !== that.model.get('projectName'),
          b = d.file.subProjectName !== that.model.get('subProjectName'),
          c = d.file.key !== that.model.get('key');
      return '' + a + b + c;
    });
    return {
      component: this.model.toJSON(),
      duplications: duplications,
      inRemovedComponent: this.options.inRemovedComponent
    };
  }
});

