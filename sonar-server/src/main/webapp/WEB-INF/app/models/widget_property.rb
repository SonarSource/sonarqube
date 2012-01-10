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
class WidgetProperty < ActiveRecord::Base
  TYPE_INTEGER = 'INTEGER'
  TYPE_BOOLEAN = 'BOOLEAN'
  TYPE_FLOAT = 'FLOAT'
  TYPE_STRING = 'STRING'
  TYPE_METRIC = 'METRIC'

  belongs_to :widget

  validates_length_of :kee, :within => 1..100
  validates_length_of :text_value, :maximum => 4000, :allow_blank => true, :allow_nil => true

  def key
    kee
  end

  def text_value
    read_attribute(:text_value) || default_text_value
  end

  def default_text_value
    java_definition.defaultValue()
  end

  def type
    @type ||=
      begin
        java_definition.type().name()
      end
  end

  def java_definition
    @java_definition ||=
      begin
        widget.java_definition.getWidgetProperty(key)
      end
  end

  def value
    WidgetProperty.text_to_value(text_value, type)
  end

  def to_hash_json
    {:key => key, :value => text_value}
  end

  def to_xml(xml=Builder::XmlMarkup.new(:indent => 0))
    xml.property do
      xml.key(prop_key)
      xml.value { xml.cdata!(text_value) }
    end
    xml
  end

  def self.text_to_value(text, type)
    case type
      when TYPE_INTEGER
        text.to_i
      when TYPE_FLOAT
        Float(text)
      when TYPE_BOOLEAN
        text=='true'
      when TYPE_METRIC
        Metric.by_key(text)
      else
        text
    end
  end

  protected
  def validate
    errors.add_to_base("Unknown property: #{key}") unless java_definition
    errors.add_to_base("Unknown type for property #{key}") unless type
    if text_value.empty?
      errors.add_to_base("#{key} is empty") unless java_definition.optional()
    else
      errors.add_to_base("#{key} is not an integer") if type==TYPE_INTEGER && !Api::Utils.is_integer?(text_value)
      errors.add_to_base("#{key} is not a decimal number") if type==TYPE_FLOAT && !Api::Utils.is_number?(text_value)
      errors.add_to_base("#{key} is not a boolean") if type==TYPE_BOOLEAN && !(text_value=="true" || text_value=="false")
    end
  end

end