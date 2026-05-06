package com.appland.appmap.test.fixture.labels;

import com.appland.appmap.annotation.Labels;

/**
 * Fixture exercising the trivial-method filter bypass for explicitly opted-in methods.
 * The integration test {@code LabelFixtureRecorderTest} drives every method here and
 * inspects the resulting AppMap recording.
 */
public class LabelFixture {
  private String value = "initial";

  /** Plain getter, no opt-in: should be filtered as trivial and NOT appear in the recording. */
  public String getPlain() {
    return value;
  }

  /** Annotated getter: should bypass the trivial filter and appear with its label. */
  @Labels("secret")
  public String getSecret() {
    return value;
  }

  /** Annotated setter: should bypass the trivial filter and appear with its label. */
  @Labels("mutator")
  public void setSecret(String value) {
    this.value = value;
  }

  /**
   * Plain getter named explicitly under {@code methods:} in {@code appmap.yml} — should bypass
   * the trivial filter on the strength of the config alone (no annotation).
   */
  public String getNamedInConfig() {
    return value;
  }

  /** Non-trivial method: always recorded via the surrounding exclude-mode package entry. */
  public String describe() {
    return "value=" + value;
  }
}
