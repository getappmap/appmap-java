package com.example.accessingdatajpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("oracle")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIfEnvironmentVariable(named = "ORACLE_URL", matches = ".*")
public class OracleRepositoryTests {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private CustomerRepository customers;

  @Autowired
  private DataSource dataSource;

  @Test
  public void testJDBC() throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM DUAL")) {
        statement.execute();
        statement.execute();
      }
      try (CallableStatement statement = connection.prepareCall("begin null; end;")) {
        statement.execute();
      }
    }
  }

  @Test
  public void testPreparedStatement() throws SQLException {
    try (Connection connection = dataSource.getConnection();
         PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM DUAL WHERE DUMMY = ?")) {
      preparedStatement.setString(1, "X");
      preparedStatement.execute();
    }
  }

  @Test
  public void testCallableStatement() throws SQLException {
    try (Connection connection = dataSource.getConnection();
         CallableStatement callableStatement = connection.prepareCall("begin :1 := 1; end;")) {
      callableStatement.registerOutParameter(1, java.sql.Types.INTEGER);
      callableStatement.execute();
    }
  }

  @Test
  public void testFindByLastName() {
    Customer customer = new Customer("Oracle", "User");
    entityManager.persist(customer);

    List<Customer> findByLastName = customers.findByLastName(customer.getLastName());

    assertThat(findByLastName).extracting(Customer::getLastName).containsOnly(customer.getLastName());
  }
}
