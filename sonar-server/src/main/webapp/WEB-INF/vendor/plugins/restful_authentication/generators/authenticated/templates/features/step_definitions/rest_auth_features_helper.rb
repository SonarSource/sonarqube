# If you have a global stories helper, move this line there:
include AuthenticatedTestHelper

# Most of the below came out of code from Ben Mabey
# http://www.benmabey.com/2008/02/04/rspec-plain-text-stories-webrat-chunky-bacon/

# These allow exceptions to come through as opposed to being caught and having non-helpful responses returned.
ActionController::Base.class_eval do
  def perform_action
    perform_action_without_rescue
  end
end
Dispatcher.class_eval do
  def self.failsafe_response(output, status, exception = nil)
    raise exception
  end
end

#
# Sugar for turning a story's attribute list into list, array, etc.
#
module ToFooFromStory
  def ToFooFromStory.fix_key key
    key.downcase.gsub(/\s+/, '_')
  end
  def ToFooFromStory.fix_value value
    return '' if !value
    value.strip!
    case
    when value =~ /^'(.*)'$/    then value = $1
    when value =~ /^"(.*)"$/    then value = $1
    when value == 'nil!'        then value = nil
    when value == 'non-nil!'    then value = be_nil
    when value =~ /^#\{(.*)\}$/ then value = eval($1)
    end
    value
  end
  # Converts a key: value list found in the steps into a hash.
  # Example:
  #   ISBN: '0967539854' and comment: 'I love this book' and Quality rating: '4'
  #   # => {"quality_rating"=>"4", "isbn"=>"0967539854", "comment"=>"I love this book"}
  def to_hash_from_story
    hsh = self.split(/,? and |, /).inject({}) do |hash_so_far, key_value|
      key, value = key_value.split(":")
      if !value then warn "Couldn't understand story '#{self}': only understood up to the part '#{hash_so_far.to_yaml}'" end
      hash_so_far.merge(ToFooFromStory::fix_key(key) => ToFooFromStory::fix_value(value))
    end
  end
  # Coverts an attribute list found in the steps into an array
  # Example:
  #   login, email, updated_at, and gravatar
  #   # => ['login', 'email', 'updated_at', 'gravatar']
  def to_array_from_story
    self.split(/,? and |, /).map do |value|
      ToFooFromStory::fix_value(value)
    end
  end
end
class String
  include ToFooFromStory
end

def instantize(string)
  instance_variable_get("@#{string}")
end

#
# Spew response onto screen -- painful but scrolling >> debugger
#
def dump_response
  # note that @request and @template won't to_yaml and that @session includes @cgi
  response_methods = response.instance_variables         - ['@request', '@template', '@cgi']
  request_methods  = response.request.instance_variables - ['@session_options_with_string_keys', '@cgi', '@session']
  response_methods.map!{|attr| attr.gsub(/^@/,'')}.sort!
  request_methods.map!{ |attr| attr.gsub(/^@/,'')}.sort!
  puts '', '*' * 75,
    response.instance_values.slice(*response_methods).to_yaml,
    "*" * 75, '',
    response.request.instance_values.slice(*request_methods).to_yaml,
    "*" * 75, ''
end
