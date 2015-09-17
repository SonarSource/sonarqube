import Marionette from 'backbone.marionette';
import './templates';

export default Marionette.LayoutView.extend({
  template: Templates['custom-measures-layout'],

  regions: {
    headerRegion: '#custom-measures-header',
    listRegion: '#custom-measures-list',
    listFooterRegion: '#custom-measures-list-footer'
  }
});


