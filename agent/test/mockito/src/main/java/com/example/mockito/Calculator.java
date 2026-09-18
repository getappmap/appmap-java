package com.example.mockito;

/** A public collaborator interface, the most common thing a project mocks. */
public interface Calculator {
  int add(int a, int b);

  String describe(String label);
}
