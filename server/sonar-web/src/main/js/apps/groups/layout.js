import Marionette from 'backbone.marionette';
import Template from './templates/groups-layout.hbs';

export default Marionette.LayoutView.extend({
  template: Template,

  regions: {
    headerRegion: '#groups-header',
    searchRegion: '#groups-search',
    listRegion: '#groups-list',
    listFooterRegion: '#groups-list-footer'
  }
});


