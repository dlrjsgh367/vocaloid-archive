package com.vocaloidarchive.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ShareCodeGeneratorTest {

  private static final Pattern BASE62 = Pattern.compile("^[A-Za-z0-9]{10}$");

  @Test
  @DisplayName("generate: 길이 10 base62 문자열")
  void generate_length10_base62() {
    String code = ShareCodeGenerator.generate();
    assertThat(code).hasSize(10);
    assertThat(BASE62.matcher(code).matches()).isTrue();
  }

  @Test
  @DisplayName("generate: 1000회 호출 시 중복 없음")
  void generate_noCollisionOverManyCalls() {
    Set<String> seen = new HashSet<>();
    for (int i = 0; i < 1000; i++) {
      seen.add(ShareCodeGenerator.generate());
    }
    assertThat(seen).hasSize(1000);
  }
}
