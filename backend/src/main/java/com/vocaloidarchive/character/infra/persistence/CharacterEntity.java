package com.vocaloidarchive.character.infra.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "characters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CharacterEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 100)
  private String name;

  @Column(name = "color_hex", nullable = false, length = 7)
  private String colorHex;

  @Column(name = "image_url", length = 500)
  private String imageUrl;

  public static CharacterEntity of(String name, String colorHex, String imageUrl) {
    CharacterEntity c = new CharacterEntity();
    c.name = name;
    c.colorHex = colorHex;
    c.imageUrl = imageUrl;
    return c;
  }
}
