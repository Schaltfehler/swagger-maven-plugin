package com.wordnik.avans;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import me.geso.avans.annotation.GET;

@Api(value = "/")
public class RootPathResource {
  @GET("/")
  @ApiOperation(value = "testingRootPathResource")
  public String testingRootPathResource() {
    return "testingRootPathResource";
  }
}
