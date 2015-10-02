import BaseView from './base-viewer-view';
import SourceViewer from '../../source-viewer/main';
import Template from '../templates/workspace-viewer.hbs';

export default BaseView.extend({
  template: Template,

  onRender: function () {
    BaseView.prototype.onRender.apply(this, arguments);
    this.showViewer();
  },

  showViewer: function () {
    var that = this,
        viewer = new SourceViewer(),
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


