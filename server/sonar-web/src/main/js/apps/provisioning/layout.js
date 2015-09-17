import Marionette from 'backbone.marionette';
import './templates';

export default Marionette.LayoutView.extend({
  template: Templates['provisioning-layout'],

  regions: {
    headerRegion: '#provisioning-header',
    searchRegion: '#provisioning-search',
    listRegion: '#provisioning-list',
    listFooterRegion: '#provisioning-list-footer'
  }
});


