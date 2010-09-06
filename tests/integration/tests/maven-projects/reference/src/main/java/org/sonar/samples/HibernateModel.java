package org.sonar.samples;

import javax.persistence.*;

@Entity
@Table(name = "foo")
public class HibernateModel implements java.io.Serializable {

  @Id
  @Column(name = "id")
  @SequenceGenerator(name = "FOO_ID_SEQ", sequenceName = "FOO_ID_SEQ")
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "FOO_ID_SEQ")
  private Integer id;

  @Column(name = "column1", updatable = true, nullable = true, length = 32)
  private String column1;

  public HibernateModel() {
  }

  public HibernateModel(String column1) {
    this.column1 = column1;
  }
}
