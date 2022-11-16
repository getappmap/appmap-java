package com.example.accessingdatajpa;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface CustomerRepository extends CrudRepository<Customer, Long> {

  List<Customer> findByLastName(String lastName);

  Customer findById(long id);

  @Query(value = "select first_name from customer1", nativeQuery = true)
  List<Customer> findFromBogusTable();

}
