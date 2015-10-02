import Popup from '../../common/popup';
import Template from '../templates/source-viewer-scm-popup.hbs';

export default Popup.extend({
  template: Template,

  events: {
    'click': 'onClick'
  },

  onRender: function () {
    Popup.prototype.onRender.apply(this, arguments);
    this.$('.bubble-popup-container').isolatedScroll();
  },

  onClick: function (e) {
    e.stopPropagation();
  }
});

