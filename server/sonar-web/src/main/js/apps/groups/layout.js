import Marionette from 'backbone.marionette';
import './templates';

export default Marionette.LayoutView.extend({
  template: Templates['groups-layout'],

  regions: {
    headerRegion: '#groups-header',
    searchRegion: '#groups-search',
    listRegion: '#groups-list',
    listFooterRegion: '#groups-list-footer'
  }
});


