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
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#

module Resourceable

  SCOPE_SET='PRJ'
  SCOPE_SPACE='DIR'
  SCOPE_ENTITY='FIL'
  SCOPES=[SCOPE_SET, SCOPE_SPACE, SCOPE_ENTITY]

  QUALIFIER_VIEW='VW'
  QUALIFIER_SUBVIEW='SVW'
  QUALIFIER_PROJECT='TRK'
  QUALIFIER_MODULE='BRC'
  QUALIFIER_DIRECTORY='DIR'
  QUALIFIER_FILE='FIL'
  QUALIFIER_PACKAGE='PAC'
  QUALIFIER_CLASS='CLA'
  QUALIFIER_UNIT_TEST_CLASS='UTS'
  QUALIFIER_LIB='LIB'
  QUALIFIERS=[QUALIFIER_VIEW, QUALIFIER_SUBVIEW, QUALIFIER_PROJECT, QUALIFIER_MODULE, QUALIFIER_DIRECTORY, QUALIFIER_PACKAGE, QUALIFIER_FILE, QUALIFIER_CLASS, QUALIFIER_UNIT_TEST_CLASS, QUALIFIER_LIB]

  def set?
    scope==SCOPE_SET
  end

  def space?
    scope==SCOPE_SPACE
  end

  def entity?
    scope==SCOPE_ENTITY
  end

  def library?
    qualifier==QUALIFIER_LIB
  end

  # No more used since SonarQube 4.0 and should not be used anymore
  def view?
    qualifier==QUALIFIER_VIEW
  end

  # No more used and should not be used
  def subview?
    qualifier==QUALIFIER_SUBVIEW
  end

  def project?
    qualifier==QUALIFIER_PROJECT
  end

  def module?
    qualifier==QUALIFIER_MODULE
  end

  def directory?
    qualifier==QUALIFIER_DIRECTORY
  end

  def file?
    qualifier==QUALIFIER_FILE
  end

  def test?
    qualifier==QUALIFIER_UNIT_TEST_CLASS
  end
  def source_code?
    java_resource_type.hasSourceCode()
  end

  def display_dashboard?
    !source_code?
  end

  def leaves_qualifiers
    @leaves_qualifiers ||=
        begin
          Api::Utils.java_facade.getResourceLeavesQualifiers(qualifier)
        end
  end

  def children_qualifiers
    @children_qualifiers ||=
        begin
          Api::Utils.java_facade.getResourceChildrenQualifiers(qualifier)
        end
  end

  def java_resource_type
    @java_resource_type ||=
        begin
          Api::Utils.java_facade.getResourceType(qualifier)
        end
  end
end
