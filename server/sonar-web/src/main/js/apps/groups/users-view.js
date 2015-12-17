import Modal from '../../components/common/modals';
import '../../components/common/select-list';
import Template from './templates/groups-users.hbs';

export default Modal.extend({
  template: Template,

  onRender: function () {
    Modal.prototype.onRender.apply(this, arguments);
    new window.SelectList({
      el: this.$('#groups-users'),
      width: '100%',
      readOnly: false,
      focusSearch: false,
      format: function (item) {
        return item.name + '<br><span class="note">' + item.login + '</span>';
      },
      queryParam: 'q',
      searchUrl: baseUrl + '/api/user_groups/users?ps=100&id=' + this.model.id,
      selectUrl: baseUrl + '/api/user_groups/add_user',
      deselectUrl: baseUrl + '/api/user_groups/remove_user',
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
    Modal.prototype.onDestroy.apply(this, arguments);
  }
});


