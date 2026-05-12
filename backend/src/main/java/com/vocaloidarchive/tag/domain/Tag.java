package com.vocaloidarchive.tag.domain;

public class Tag {

  private final Long id;
  private final String name;

  private Tag(Long id, String name) {
    this.id = id;
    this.name = name;
  }

  public static Tag newTag(String normalizedName) {
    return new Tag(null, normalizedName);
  }

  public static Tag reconstitute(Long id, String name) {
    return new Tag(id, name);
  }

  public Long getId() { return id; }
  public String getName() { return name; }
}
