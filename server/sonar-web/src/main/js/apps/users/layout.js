import Marionette from 'backbone.marionette';
import './templates';

export default Marionette.LayoutView.extend({
  template: Templates['users-layout'],

  regions: {
    headerRegion: '#users-header',
    searchRegion: '#users-search',
    listRegion: '#users-list',
    listFooterRegion: '#users-list-footer'
  }
});


