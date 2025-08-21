token = User.find_by_username('root').personal_access_tokens.create(scopes: [:api], name: 'token');
token.set_token('token-here-456');
token.expires_at = Date.today+10.day
token.save!;
token_read = User.find_by_username('root').personal_access_tokens.create(scopes: [:read_user], name: 'token_read');
token_read.set_token('token-read-123');
token_read.expires_at = Date.today+10.day
token_read.save!;
user = User.find_by_username('root');
user.password = 'eng-YTU1ydh6kyt7tjd';
user.password_confirmation = 'eng-YTU1ydh6kyt7tjd';
user.save!;
