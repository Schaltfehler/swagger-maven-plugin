package com.wordnik.sample.model.avans;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Alien {
  @JsonProperty
  private String id;
  @JsonProperty
  private String category;
  @JsonProperty
  private String name;
}
