#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2013 SonarSource
# mailto:contact AT sonarsource DOT com
#
# SonarQube is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# SonarQube is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#
require 'time'
require 'uri'

class Api::Utils

  # Format dateTime to ISO format (yyyy-MM-dd'T'HH:mm:ssZ)
  #
  # -- Revisions
  # Added in 2.8
  # Support java.util.Date in 3.6
  #
  def self.format_datetime(datetime)
    return nil unless datetime
    dt = datetime.is_a?(Java::JavaUtil::Date) ? java_to_ruby_datetime(datetime) : datetime
    dt.strftime("%Y-%m-%dT%H:%M:%S%z")
  end

  # Format dateTime to ISO format (yyyy-MM-dd)
  #
  # Added in 3.6
  # Support java.util.Date
  #
  def self.format_date(datetime)
    return nil unless datetime
    dt = datetime.is_a?(Java::JavaUtil::Date) ? java_to_ruby_datetime(datetime) : datetime
    dt.strftime("%Y-%m-%d")
  end

  def self.parse_datetime(datetime_string, default_is_now=true)
    if datetime_string.blank?
      return (default_is_now ? Time.now : nil)
    end
    Time.parse(datetime_string)
  end

  # Convert java.util.Date to ruby Time
  #
  # -- Revisions
  # Added in 3.6
  def self.java_to_ruby_datetime(java_date)
    java_date && Time.at(java_date.time/1000)
  end

  def self.is_number?(s)
    true if Float(s) rescue false
  end

  def self.is_integer?(s)
    s.to_s =~ /\A[+-]?\d+\Z/
  end

  def self.is_boolean?(s)
    s == 'true' || s == 'false'
  end

  def self.is_regexp?(s)
    Regexp.new(s)
    true
  rescue
    false
  end

  def self.markdown_to_html(markdown='')
    Internal.text.markdownToHtml(markdown)
  end

  # Splits a string into an array of lines
  # For history reference:
  #   - http://jira.codehaus.org/browse/SONAR-2282 first modified the behaviour to keep the trailing lines
  #   - then http://jira.codehaus.org/browse/SONAR-3003 reverted this modification to remove potential last empty line
  #   - then http://jira.codehaus.org/browse/SONAR-3896 reactivate this modification to display last empty line
  def self.split_newlines(input)
    input.split(/\r?\n|\r/, -1)
  end

  def self.convert_string_to_unix_newlines(input)
    # Don't use '\n' here
    # See http://jira.codehaus.org/browse/SONAR-2571
    split_newlines(input).join("\n")
  end

  #
  # i18n
  # Since 2.10
  def self.message(key, options={})
    default = options[:default]
    params = options[:params]||[]
    java_facade.getMessage(I18n.locale, key, default, params.to_java)
  end

  #
  # Options :
  # - backtrace: append backtrace if true. Default value is false.
  #
  def self.exception_message(exception, options={})
    cause = exception
    if exception.is_a?(NativeException) && exception.respond_to?(:cause)
      cause = exception.cause
    end
    result = (cause.respond_to?(:message) ? "#{cause.message}\n" : "#{cause}\n")
    if options[:backtrace]==true && cause.respond_to?(:backtrace)
      result << "\t" + cause.backtrace.join("\n\t") + "\n"
    end
    result
  end

  # Returns a new array created by sorting arr
  # Since Sonar 3.0
  #
  # Examples :
  # Api::Utils.insensitive_sort(['foo', 'bar'])
  # Api::Utils.insensitive_sort([foo, bar]) { |elt| elt.nullable_field_to_compare }
  #
  def self.insensitive_sort(arr)
    if block_given?
      arr.sort do |a, b|
        a_string=yield(a) || ''
        b_string=yield(b) || ''
        a_string.downcase <=> b_string.downcase || a_string <=> b_string
      end
    else
      arr.sort do |a, b|
        a_string=a || ''
        b_string=b || ''
        a_string.downcase <=> b_string.downcase || a_string <=> b_string
      end
    end
  end


  # Sorts arr
  # Since Sonar 3.0
  #
  # Examples :
  # Api::Utils.insensitive_sort!(['foo', 'bar'])
  # Api::Utils.insensitive_sort!([foo, bar]) { |elt| elt.nullable_field_to_compare }
  #
  def self.insensitive_sort!(arr)
    if block_given?
      arr.sort! do |a, b|
        a_string=yield(a) || ''
        b_string=yield(b) || ''
        a_string.downcase <=> b_string.downcase || a_string <=> b_string
      end
    else
      arr.sort! do |a, b|
        a_string=a || ''
        b_string=b || ''
        a_string.downcase <=> b_string.downcase || a_string <=> b_string
      end
    end
  end

  #
  # Since Sonar 3.0
  #
  def self.valid_period_index?(index)
    Api::Utils.is_integer?(index) && index.to_i > 0 && index.to_i <6
  end

  #
  # Since Sonar 3.1
  #
  # Read content of HTTP POST request. Example: read_post_request_param(params[:backup])
  #
  def self.read_post_request_param(param_value)
    if param_value
      param_value.respond_to?(:read) ? param_value.read : param_value
    else
      nil
    end
  end

  def self.java_facade
    Java::OrgSonarServerUi::JRubyFacade.getInstance()
  end

  def self.languages
    java_facade.getLanguages().sort_by(&:getName)
  end

  def self.language(key)
    languages.find { |language| language.key == key }
  end

  def self.language_name(key)
    l = language(key)
    l ? l.name : key
  end

  # Label of global periods
  # index is in [1..3]
  def self.period_label(index)
    java_facade.getPeriodLabel(index)
  end

  # Abbreviated label of global periods
  # index is in [1..3]
  def self.period_abbreviation(index)
    java_facade.getPeriodAbbreviation(index)
  end

  # Prevent CSRF
  def self.absolute_to_relative_url(url)
    begin
      URI(url).request_uri
    rescue
      url
    end
  end
end
