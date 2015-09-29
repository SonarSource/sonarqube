import BaseView from './base-viewer-view';
import SourceViewer from 'components/source-viewer/main';
import '../templates';

export default BaseView.extend({
  template: Templates['workspace-viewer'],

  onRender: function () {
    BaseView.prototype.onRender.apply(this, arguments);
    this.showViewer();
  },

  showViewer: function () {
    var RealSourceViewer = SourceViewer.on ? SourceViewer : require('components/source-viewer/main');
    var that = this,
        viewer = new RealSourceViewer(),
        options = this.model.toJSON();
    viewer.open(this.model.get('uuid'), { workspace: true });
    viewer.on('loaded', function () {
      that.model.set({
        name: viewer.model.get('name'),
        q: viewer.model.get('q')
      });
      if (options.line != null) {
        viewer.highlightLine(options.line);
        viewer.scrollToLine(options.line);
      }
    });
    this.viewerRegion.show(viewer);
  }
});


