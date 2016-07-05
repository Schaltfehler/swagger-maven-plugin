package com.wordnik.avans;

import com.wordnik.sample.JavaRestResourceUtil;
import com.wordnik.sample.data.PetData;
import com.wordnik.sample.model.Pet;
import com.wordnik.sample.model.PetName;
import io.swagger.annotations.*;
import me.geso.avans.annotation.*;

import javax.ws.rs.core.Response;

@Api(tags = "pet", produces = "application/json,application/xml", authorizations = @Authorization(value = "petstore_auth", scopes = {
  @AuthorizationScope(scope = "write:pets", description = "modify pets in your account"),
  @AuthorizationScope(scope = "read:pets", description = "read your pets")
}))

public class PetResource {
  static PetData petData = new PetData();
  static JavaRestResourceUtil ru = new JavaRestResourceUtil();

  @GET("/pet/{petId : [0-9]}")
  @ApiOperation(value = "Find pet by ID", notes = "Returns a pet when ID < 10.  ID > 10 or nonintegers will simulate API error conditions", response = Pet.class, authorizations = @Authorization("api_key"))
  @ApiResponses({@ApiResponse(code = 400, message = "Invalid ID supplied"),
    @ApiResponse(code = 404, message = "Pet not found")})
  public Response getPetById(
    @ApiParam(value = "ID of pet that needs to be fetched", allowableValues = "range[1,5]", required = true) @PathParam("petId") Long petId)
    throws com.wordnik.sample.exception.NotFoundException {
    Pet pet = petData.getPetbyId(petId);
    if (pet != null) {
      return Response.ok().entity(pet).build();
    } else {
      throw new com.wordnik.sample.exception.NotFoundException(404, "Pet not found");
    }
  }

  //contrived example test case for swagger-maven-plugin issue #304
  @GET("/pet/{startId : [0-9]{1,2}}:{endId : [0-9]{1,2}}")
  @ApiOperation(value = "Find pet(s) by ID", notes = "This is a contrived example of a path segment containing multiple path parameters, separated by a character which may be present in the path parameter template. You may think that it returns a range of pets from startId to endId, inclusive, but it doesn't.", response = Pet.class, authorizations = @Authorization(value = "api_key"))
  @ApiResponses(value = {@ApiResponse(code = 400, message = "Invalid ID supplied"),
    @ApiResponse(code = 404, message = "Pet not found")})
  public Response getPetsById(
    @ApiParam(value = "start ID of pets that need to be fetched", allowableValues = "range[1,99]", required = true) @PathParam("startId") Long startId,
    @ApiParam(value = "end ID of pets that need to be fetched", allowableValues = "range[1,99]", required = true) @PathParam("endId") Long endId)
    throws com.wordnik.sample.exception.NotFoundException {
    Pet pet = petData.getPetbyId(startId);
    if (pet != null) {
      return Response.ok().entity(pet).build();
    } else {
      throw new com.wordnik.sample.exception.NotFoundException(404, "Pet not found");
    }
  }

  @POST("/pet")
  @ApiOperation(value = "Add a new pet to the store", consumes = "application/json,application/xml")
  @ApiResponses(value = {@ApiResponse(code = 405, message = "Invalid input")})
  public Response addPet(
    @ApiParam(value = "Pet object that needs to be added to the store", required = true) Pet pet) {
    Pet updatedPet = petData.addPet(pet);
    return Response.ok().entity(updatedPet).build();
  }

  @GET("/pet/pets/{petName : [^/]*}")
  @ApiOperation(value = "Finds Pets by name", response = Pet.class, responseContainer = "List")
  @ApiResponses(value = {
    @ApiResponse(code = 400, message = "Invalid status value")})
  public Response findPetByPetName(
    @ApiParam(value = "petName", required = true) @PathParam("petName") PetName petName) {
    return Response.ok(petData.getPetbyId(1)).build();
  }

  @GET("/pet/findByStatus")
  @ApiOperation(value = "Finds Pets by status", notes = "Multiple status values can be provided with comma seperated strings", response = Pet.class, responseContainer = "List")
  @ApiResponses(@ApiResponse(code = 400, message = "Invalid status value"))
  public Response findPetsByStatus(
    @ApiParam(value = "Status values that need to be considered for filter", required = true, defaultValue = "available", allowableValues = "available,pending,sold", allowMultiple = true) @Param("status") String status) {
    return Response.ok(petData.findPetByStatus(status)).build();
  }

  @GET("/pet/findByTags")
  @ApiOperation(value = "Finds Pets by tags", notes = "Muliple tags can be provided with comma seperated strings. Use tag1, tag2, tag3 for testing.", response = Pet.class, responseContainer = "List")
  @ApiResponses(@ApiResponse(code = 400, message = "Invalid tag value"))
  @Deprecated
  public Response findPetsByTags(
    @ApiParam(value = "Tags to filter by", required = true, allowMultiple = true) @Param("tags") String tags) {
    return Response.ok(petData.findPetByTags(tags)).build();
  }

  @POST("/pet/{petId}")
  @ApiOperation(value = "Updates a pet in the store with form data", consumes = "application/x-www-form-urlencoded")
  @ApiResponses(@ApiResponse(code = 405, message = "Invalid input"))
  public Response updatePetWithForm(
    @BeanParam MyBean myBean) {
    System.out.println(myBean.getName());
    System.out.println(myBean.getStatus());
    return Response.ok().entity(new com.wordnik.sample.model.ApiResponse(200, "SUCCESS")).build();
  }


  @GET("/pet")
  @ApiOperation(value = "Returns pet", response = Pet.class, produces = "application/json")
  public Pet get(
    @ApiParam(hidden = true, name = "hiddenParameter") @Param("hiddenParameter") String hiddenParameter) {
    return new Pet();
  }

  @GET("/pet/test")
  @ApiOperation(value = "Test pet as json string in query", response = Pet.class, produces = "application/json")
  public Pet test(
    @ApiParam(value = "describe Pet in json here") @Param("pet") Pet pet) {
    return new Pet();
  }

  @GET("/pet/test/extensions")
  @ApiOperation(value = "testExtensions", produces = "text/plain", extensions = {
    @Extension(name = "firstExtension", properties = {
      @ExtensionProperty(name = "extensionName1", value = "extensionValue1"),
      @ExtensionProperty(name = "extensionName2", value = "extensionValue2")}),
    @Extension(properties = {
      @ExtensionProperty(name = "extensionName3", value = "extensionValue3")})
  })
  public Pet testingExtensions() {
    return new Pet();
  }

  @ApiOperation(value = "Test apiimplicitparams", response = Pet.class, produces = "application/json")
  @GET("/pet/test/apiimplicitparams/{path-test-name}")
  @ApiImplicitParams(value = {
    @ApiImplicitParam(name = "header-test-name", value = "header-test-value", required = true, dataType = "string", paramType = "header", defaultValue = "z"),

    @ApiImplicitParam(name = "path-test-name", value = "path-test-value", required = true, dataType = "string", paramType = "path", defaultValue = "path-test-defaultValue"),

    @ApiImplicitParam(name = "body-test-name", value = "body-test-value", required = true, dataType = "com.wordnik.sample.model.Pet", paramType = "body")
  })
  public Pet testapiimplicitparams() {
    return new Pet();
  }

  @ApiOperation(value = "Test testFormApiImplicitParams", response = Pet.class, produces = "application/json")
  @GET("/pet/test/testFormApiImplicitParams")
  @ApiImplicitParams(value = {
    @ApiImplicitParam(name = "form-test-name", value = "form-test-value", allowMultiple = true, required = true, dataType = "string", paramType = "form", defaultValue = "form-test-defaultValue")
  })
  public Pet testFormApiImplicitParams() {
    return new Pet();
  }

  @ApiOperation(value = "testingHiddenApiOperation", hidden = true, produces = "application/json")
  @GET("/pet")
  public String testingHiddenApiOperation() {
    return "testingHiddenApiOperation";
  }

  @ApiOperation(value = "testingBasicAuth", authorizations = @Authorization(value = "basicAuth"))
  @GET("/pet/test/testingBasicAuth")
  public String testingBasicAuth() {
    return "testingBasicAuth";
  }

  @ApiOperation(value = "testingArrayResponse")
  @ApiResponses(@ApiResponse(code = 200, message = "array", response = Pet.class, responseContainer = "List"))
  @GET("/pet/test/testingArrayResponse")
  public Response testingArrayResponse() {
    return null;
  }
}
