/*
 * Sonar, entreprise quality control tool.
 * Copyright (C) 2007-2008 Hortis-GRC SA
 * mailto:be_agile HAT hortis DOT ch
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package duplicated_lines_within_same_class;

public class DuplicatedLinesInSameClass {

  public DuplicatedLinesInSameClass() {
  }

  public void duplicatedMethodInSameClass1() {
    String temp = "";
    for (int i=0; i<10; i++){
      temp += "say something"+i;
    }
    for (int i=0; i<20; i++){
      temp += "say nothing"+i;
    }
    for (int i=0; i<30; i++){
      temp += "always say nothing"+i;
    }
    for (int i=0; i<40; i++){
      temp += "really nothing to say "+i;
    }
    for (int i=0; i<50; i++){
      temp += "really really nothing to say "+i;
    }
    for (int i=0; i<60; i++){
      temp += ".. "+i;
    }
    for (int i=0; i<70; i++){
      temp += "you say something? "+i;
    }
    for (int i=0; i<80; i++){
      temp += "ah no..."+i;
    }
    for (int i=0; i<90; i++){
      temp += "bye"+i;
    }
  }

  public void duplicatedMethodInSameClass2() {
    String temp = "";
    for (int i=0; i<10; i++){
      temp += "say something"+i;
    }
    for (int i=0; i<20; i++){
      temp += "say nothing"+i;
    }
    for (int i=0; i<30; i++){
      temp += "always say nothing"+i;
    }
    for (int i=0; i<40; i++){
      temp += "really nothing to say "+i;
    }
    for (int i=0; i<50; i++){
      temp += "really really nothing to say "+i;
    }
    for (int i=0; i<60; i++){
      temp += ".. "+i;
    }
    for (int i=0; i<70; i++){
      temp += "you say something? "+i;
    }
    for (int i=0; i<80; i++){
      temp += "ah no..."+i;
    }
    for (int i=0; i<90; i++){
      temp += "bye"+i;
    }  
  }

}
