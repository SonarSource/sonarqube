import Modal from 'components/common/modals';
import 'components/common/select-list';
import './templates';

export default Modal.extend({
  template: Templates['groups-users'],

  onRender: function () {
    this._super();
    new window.SelectList({
      el: this.$('#groups-users'),
      width: '100%',
      readOnly: false,
      focusSearch: false,
      format: function (item) {
        return item.name + '<br><span class="note">' + item.login + '</span>';
      },
      queryParam: 'q',
      searchUrl: baseUrl + '/api/usergroups/users?ps=100&id=' + this.model.id,
      selectUrl: baseUrl + '/api/usergroups/add_user',
      deselectUrl: baseUrl + '/api/usergroups/remove_user',
      extra: {
        id: this.model.id
      },
      selectParameter: 'login',
      selectParameterValue: 'login',
      parse: function (r) {
        this.more = false;
        return r.users;
      }
    });
  },

  onDestroy: function () {
    this.model.collection.refresh();
    this._super();
  }
});


