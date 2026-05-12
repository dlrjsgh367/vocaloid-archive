package com.vocaloidarchive.tag.infra.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TagEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 50)
  private String name;

  private TagEntity(String name) {
    this.name = name;
  }

  public static TagEntity of(String normalizedName) {
    return new TagEntity(normalizedName);
  }
}
