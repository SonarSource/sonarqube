import _ from 'underscore';
import Popup from '../../components/common/popup';
import Template from './templates/update-center-plugin-changelog.hbs';

export default Popup.extend({
  template: Template,

  onRender: function () {
    Popup.prototype.onRender.apply(this, arguments);
    this.$('.bubble-popup-container').isolatedScroll();
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
  },

  onDestroy: function () {
    Popup.prototype.onDestroy.apply(this, arguments);
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  serializeData: function () {
    return _.extend(Popup.prototype.serializeData.apply(this, arguments), {
      // if there is no status, this is a new plugin
      // => force COMPATIBLE status
      status: this.model.get('status') || 'COMPATIBLE'
    });
  }
});


