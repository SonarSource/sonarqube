import Marionette from 'backbone.marionette';
import './templates';

export default Marionette.LayoutView.extend({
  template: Templates['computation-layout'],

  regions: {
    headerRegion: '#computation-header',
    searchRegion: '#computation-search',
    listRegion: '#computation-list',
    listFooterRegion: '#computation-list-footer'
  }
});


