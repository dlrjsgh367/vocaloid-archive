package com.vocaloidarchive.migration;

import com.vocaloidarchive.support.AbstractMysqlContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FlywayMigrationTest extends AbstractMysqlContainerTest {

  @Autowired JdbcTemplate jdbc;

  @Test
  void givenFlywayMigrated_whenInspected_thenAllExpectedTablesExist() {
    List<String> tables = jdbc.queryForList(
        "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE()",
        String.class);

    assertThat(tables).contains(
        "users", "refresh_tokens", "songs", "characters",
        "song_characters", "tags", "song_tags", "playlists",
        "playlist_songs", "likes", "comments");
  }

  @Test
  void givenSongsTable_whenColumnsInspected_thenMoodIsEnumStringAndPlayCountDefaults() {
    Integer playCount = jdbc.queryForObject(
        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
        "WHERE TABLE_NAME='songs' AND COLUMN_NAME='play_count' AND COLUMN_DEFAULT='0'",
        Integer.class);
    assertThat(playCount).isEqualTo(1);
  }
}
