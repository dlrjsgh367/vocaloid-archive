package com.vocaloidarchive.migration;

import com.vocaloidarchive.support.AbstractMysqlContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CharacterSeedTest extends AbstractMysqlContainerTest {

  @Autowired JdbcTemplate jdbc;

  @Test
  void givenV2Migrated_whenCharactersCounted_thenAtLeastTenSeeded() {
    Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM characters", Integer.class);
    assertThat(count).isGreaterThanOrEqualTo(10);
  }

  @Test
  void givenSeed_whenMikuLookedUp_thenColorMatchesSignature() {
    String color = jdbc.queryForObject(
        "SELECT color_hex FROM characters WHERE name = ?", String.class, "하츠네 미쿠");
    assertThat(color).isEqualTo("#39C5BB");
  }
}
