import Marionette from 'backbone.marionette';
import './templates';

export default Marionette.LayoutView.extend({
  template: Templates['metrics-layout'],

  regions: {
    headerRegion: '#metrics-header',
    listRegion: '#metrics-list',
    listFooterRegion: '#metrics-list-footer'
  }
});


