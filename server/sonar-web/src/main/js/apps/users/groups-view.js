import Modal from '../../components/common/modals';
import '../../components/common/select-list';
import Template from './templates/users-groups.hbs';

export default Modal.extend({
  template: Template,

  onRender: function () {
    Modal.prototype.onRender.apply(this, arguments);
    new window.SelectList({
      el: this.$('#users-groups'),
      width: '100%',
      readOnly: false,
      focusSearch: false,
      format: function (item) {
        return item.name + '<br><span class="note">' + item.description + '</span>';
      },
      queryParam: 'q',
      searchUrl: baseUrl + '/api/users/groups?ps=100&login=' + this.model.id,
      selectUrl: baseUrl + '/api/user_groups/add_user',
      deselectUrl: baseUrl + '/api/user_groups/remove_user',
      extra: {
        login: this.model.id
      },
      selectParameter: 'id',
      selectParameterValue: 'id',
      parse: function (r) {
        this.more = false;
        return r.groups;
      }
    });
  },

  onDestroy: function () {
    this.model.collection.refresh();
    Modal.prototype.onDestroy.apply(this, arguments);
  }
});


