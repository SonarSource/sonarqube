#
# Sonar, entreprise quality control tool.
# Copyright (C) 2009 SonarSource SA
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

  belongs_to :widget

  validates_length_of       :kee, :within => 1..100
  validates_length_of       :text_value,   :maximum => 4000, :allow_blank => true, :allow_nil => true

  def key
    kee
  end

  def value
    text_value
  end

  def to_hash_json
    {:key => key, :value => value.to_s}
  end

  def to_xml(xml=Builder::XmlMarkup.new(:indent => 0))
    xml.property do
      xml.key(prop_key)
      xml.value {xml.cdata!(text_value.to_s)}
    end
    xml
  end

  def self.validate_definition(definition, value)
    errors=[]
    if value.empty?
      errors<<"Missing value" unless definition.optional()
    else
      errors<<"Please type an integer (example: 123)" if definition.type()==TYPE_INTEGER && value.to_i.to_s!=value
      if definition.type()==TYPE_FLOAT
        begin
          Float(value)
        rescue
          errors<<"Please type a number (example: 123.45)"
        end
      end
      errors<<"Please check value" if definition.type()==TYPE_BOOLEAN && !(value=="true" || value=="false")
    end
    errors
  end
end