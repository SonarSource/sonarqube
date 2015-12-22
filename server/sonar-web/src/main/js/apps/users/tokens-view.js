import $ from 'jquery';
import _ from 'underscore';
import Clipboard from 'clipboard';

import Modal from '../../components/common/modals';
import Template from './templates/users-tokens.hbs';
import { getTokens, generateToken, revokeToken } from '../../api/user-tokens';


export default Modal.extend({
  template: Template,

  events () {
    return _.extend(Modal.prototype.events.apply(this, arguments), {
      'submit .js-generate-token-form': 'onGenerateTokenFormSubmit',
      'submit .js-revoke-token-form': 'onRevokeTokenFormSubmit'
    });
  },

  initialize () {
    Modal.prototype.initialize.apply(this, arguments);
    this.tokens = null;
    this.newToken = null;
    this.errors = [];
    this.requestTokens();
  },

  requestTokens () {
    return getTokens(this.model.id).then(tokens => {
      this.tokens = tokens;
      this.render();
    });
  },

  onGenerateTokenFormSubmit (e) {
    e.preventDefault();
    this.errors = [];
    this.newToken = null;
    let tokenName = this.$('.js-generate-token-form input').val();
    generateToken(this.model.id, tokenName)
        .then(response => {
          this.newToken = response.token;
          this.requestTokens();
        })
        .catch(error => {
          error.response.json().then(response => {
            this.errors = response.errors;
            this.render();
          });
        });
  },

  onRevokeTokenFormSubmit(e) {
    e.preventDefault();
    let tokenName = $(e.currentTarget).data('token');
    let token = _.findWhere(this.tokens, { name: `${tokenName}` });
    if (token) {
      if (token.deleting) {
        revokeToken(this.model.id, tokenName).then(this.requestTokens.bind(this));
      } else {
        token.deleting = true;
        this.render();
      }
    }
  },

  onRender () {
    Modal.prototype.onRender.apply(this, arguments);
    let copyButton = this.$('.js-copy-to-clipboard');
    if (copyButton.length) {
      let clipboard = new Clipboard(copyButton.get(0));
      clipboard.on('success', () => {
        copyButton.tooltip({ title: 'Copied!', placement: 'bottom', trigger: 'manual' }).tooltip('show');
        setTimeout(() => copyButton.tooltip('hide'), 1000);
      });
    }
    this.newToken = null;
  },

  serializeData() {
    return _.extend(Modal.prototype.serializeData.apply(this, arguments), {
      tokens: this.tokens,
      newToken: this.newToken,
      errors: this.errors
    });
  }

});
