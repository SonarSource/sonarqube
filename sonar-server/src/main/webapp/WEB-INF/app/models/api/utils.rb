#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2012 SonarSource
# mailto:contact AT sonarsource DOT com
#
# Sonar is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# Sonar is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with Sonar; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#
require 'time'

class Api::Utils

  # Format dateTime to ISO format
  def self.format_datetime(datetime)
    datetime.strftime("%Y-%m-%dT%H:%M:%S%z")
  end

  def self.parse_datetime(datetime_string, default_is_now=true)
    if datetime_string.blank?
      return (default_is_now ? Time.now : nil)
    end
    Time.parse(datetime_string)
  end

  def self.is_number?(s)
    true if Float(s) rescue false
  end

  def self.is_integer?(s)
    s.to_s =~ /\A[+-]?\d+\Z/
  end

  def self.markdown_to_html(markdown)
    markdown ? Java::OrgSonarServerUi::JRubyFacade.markdownToHtml(ERB::Util.html_escape(markdown)) : ''
  end

  # splits a string into an array of lines
  def self.split_newlines(input)
    # Don't limit number of returned fields and don't suppress trailing empty fields by setting second parameter to negative value.
    # See http://jira.codehaus.org/browse/SONAR-2282
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
    Java::OrgSonarServerUi::JRubyFacade.getInstance().getMessage(I18n.locale, key, default, params.to_java)
  end

  def self.exception_message(exception)
    result = (exception.respond_to?(:message) ? "#{exception.message}\n" : "#{exception}\n")
    if exception.respond_to? :backtrace
      result << "\t" + exception.backtrace.join("\n\t") + "\n"
    end
    result
  end
end
