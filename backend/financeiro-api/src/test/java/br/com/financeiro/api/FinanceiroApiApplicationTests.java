package br.com.financeiro.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class FinanceiroApiApplicationTests {

  @Test
  void contextLoads() {}
}
