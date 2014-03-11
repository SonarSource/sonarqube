 #
 # SonarQube, open source software quality management tool.
 # Copyright (C) 2008-2014 SonarSource
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
 # License along with {library}; if not, write to the Free Software
 # Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 #
class EventCategory
  PREFIX='sonar.events.category.'
  NAME_MAX_LENGTH=511 - PREFIX.length

  KEY_VERSION='Version'
  KEY_ALERT='Alert'
  KEY_PROFILE='Profile'
  KEY_OTHER='Other'

  def initialize(name=nil, description=nil)
    @name=name
    @description=description
  end

  def name
    @name ? @name.strip : nil
  end

  def description
    @description
  end

  def save
    errors=validate
    if errors.empty?
      Property.set(EventCategory.property_key(name), description)
    end
    errors
  end

  def delete
    Property.clear(EventCategory.property_key(name))
    Event.update_all("category=null", "category='#{name}'")
  end

  def rename(from)
    errors=save
    if errors.empty?
      from_categ=EventCategory.category(from)
      if from_categ
        Event.update_all({:category => name}, "category='#{from_categ.name}'")
        from_categ.delete
      end
    end
    errors    
  end

  def valid?
    validate.length==0
  end

  def validate
    errors=[]
    errors<<'Name is empty' if name.blank?
    errors<<"Name is too long (#{NAME_MAX_LENGTH} characters max.)" if name.length>NAME_MAX_LENGTH
    errors<<'Description is empty' if description.blank?
    errors<<'Core categories can not be updated or deleted' if not editable?
    errors
  end

  def <=>(other)
    name <=> other.name  
  end

  def editable?
    !([KEY_VERSION, KEY_ALERT, KEY_PROFILE, KEY_OTHER].include?(name))
  end

  def self.defaults
    [
      EventCategory.new(KEY_VERSION, 'Application version'),
      EventCategory.new(KEY_ALERT, 'Alert'),
      EventCategory.new(KEY_PROFILE, 'Profile change'),
      EventCategory.new(KEY_OTHER, 'Other events')
    ]
  end

  def self.other_category
    EventCategory.new(KEY_OTHER, 'Other events')
  end

  def self.categories(include_defaults=false)
    events= (include_defaults ? defaults : [])

    Property.by_key_prefix(PREFIX).each do |property|
      name=property.key[PREFIX.size..-1]
      events<<EventCategory.new(name, property.value)
    end

    events.sort{|e1,e2| e1.name<=>e2.name}
  end

  def self.category(name)
    desc=Property.value(property_key(name))
    desc ? EventCategory.new(name, desc) : nil
  end

  private

  def self.property_key(name)
    "#{PREFIX}#{name}"
  end
end
