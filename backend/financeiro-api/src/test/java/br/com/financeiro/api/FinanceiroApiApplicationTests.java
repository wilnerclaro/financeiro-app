package br.com.financeiro.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = "app.jwt.segredo=segredo-apenas-para-testes-com-mais-de-32-bytes")
@Import(TestcontainersConfiguration.class)
class FinanceiroApiApplicationTests {

  @Test
  void contextLoads() {}
}
