import Modal from 'components/common/modals';
import 'components/common/select-list';
import './templates';

export default Modal.extend({
  template: Templates['users-groups'],
  itemTemplate: Templates['users-group'],

  onRender: function () {
    this._super();
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
      selectUrl: baseUrl + '/api/usergroups/add_user',
      deselectUrl: baseUrl + '/api/usergroups/remove_user',
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


