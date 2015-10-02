import _ from 'underscore';
import Popup from '../../components/common/popup';
import Template from './templates/update-center-plugin-changelog.hbs';

export default Popup.extend({
  template: Template,

  onRender: function () {
    this._super();
    this.$('.bubble-popup-container').isolatedScroll();
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
  },

  onClose: function () {
    this._super();
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  serializeData: function () {
    return _.extend(this._super(), {
      // if there is no status, this is a new plugin
      // => force COMPATIBLE status
      status: this.model.get('status') || 'COMPATIBLE'
    });
  }
});


