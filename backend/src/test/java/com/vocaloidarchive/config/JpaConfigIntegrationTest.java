package com.vocaloidarchive.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.vocaloidarchive.support.AbstractMysqlContainerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JpaConfigIntegrationTest extends AbstractMysqlContainerTest {

  @Autowired(required = false) JPAQueryFactory jpaQueryFactory;

  @Test
  void givenSpringContext_whenLoaded_thenJpaQueryFactoryBeanIsRegistered() {
    assertThat(jpaQueryFactory).isNotNull();
  }
}
