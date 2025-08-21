external_url 'https://localhost'
nginx['redirect_http_to_https'] = true
registry_nginx['redirect_http_to_https'] = true
letsencrypt['enable'] = false
nginx['ssl_certificate'] = '/etc/gitlab/ssl/localhost.crt'
nginx['ssl_certificate_key'] = '/etc/gitlab/ssl/localhost.key'

