import md5 from 'blueimp-md5';
import Handlebars from 'handlebars/runtime';

module.exports = function (email, size) {
  // double the size for high pixel density screens
  var emailHash = md5.md5((email || '').trim()),
      url = ('' + window.SS.lf.gravatarServerUrl)
          .replace('{EMAIL_MD5}', emailHash)
          .replace('{SIZE}', size * 2);
  return new Handlebars.default.SafeString(
      '<img class="rounded" src="' + url + '" width="' + size + '" height="' + size + '" alt="' + email + '">'
  );
};
