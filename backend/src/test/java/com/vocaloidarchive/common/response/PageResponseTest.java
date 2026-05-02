package com.vocaloidarchive.common.response;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

  @Test
  void givenSpringPage_whenFrom_thenContentAndMetadataMapped() {
    var springPage = new PageImpl<>(List.of("a", "b", "c"), PageRequest.of(1, 3), 10);

    PageResponse<String> response = PageResponse.from(springPage);

    assertThat(response.content()).containsExactly("a", "b", "c");
    assertThat(response.page()).isEqualTo(1);
    assertThat(response.size()).isEqualTo(3);
    assertThat(response.totalElements()).isEqualTo(10);
    assertThat(response.totalPages()).isEqualTo(4);
  }
}
