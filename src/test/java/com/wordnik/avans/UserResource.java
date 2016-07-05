package com.wordnik.avans;

import com.wordnik.sample.data.UserData;
import com.wordnik.sample.exception.ApiException;
import com.wordnik.sample.exception.NotFoundException;
import com.wordnik.sample.model.User;
import io.swagger.annotations.*;
import me.geso.avans.annotation.GET;
import me.geso.avans.annotation.POST;
import me.geso.avans.annotation.Param;
import me.geso.avans.annotation.PathParam;

import javax.ws.rs.core.Response;

@SwaggerDefinition(host = "www.example.com:8080",
  basePath = "/api",
  info = @Info(title = "Swagger Maven Avans Plugin Sample",
    version = "v1",
    description = "Processed with AvansReader",
    termsOfService = "http://www.github.com/kongchen/swagger-maven-plugin",
    contact = @Contact(name = "Mr Example", email = "example@gmail.com", url = "http://example.com"),
    license = @License(name = "Apache 2.0", url = "http://www.apache.org/licenses/LICENSE-2.0.html")
  )
)

@Api(value = "user", description = "Operations about user", consumes = "application/json")
public class UserResource {
  static UserData userData = new UserData();

  @POST("/user")
  @ApiOperation(value = "Create user", notes = "This can only be done by the logged in user.", position = 1)
  public Response createUser(
    @ApiParam(value = "Created user object", required = true) User user) {
    userData.addUser(user);
    return Response.ok().entity("").build();
  }

  @POST("/user/createWithArray")
  @ApiOperation(value = "Creates list of users with given input array", position = 2)
  public Response createUsersWithArrayInput(@ApiParam(value = "List of user object", required = true) User[] users) {
    for (User user : users) {
      userData.addUser(user);
    }
    return Response.ok().entity("").build();
  }

  @POST("/user/createWithList")
  @ApiOperation(value = "Creates list of users with given input array", position = 3)
  public Response createUsersWithListInput(
    @ApiParam(value = "List of user object", required = true) java.util.List<User> users) {
    for (User user : users) {
      userData.addUser(user);
    }
    return Response.ok().entity("").build();
  }

  @GET("/user/{username}")
  @ApiOperation(value = "Get user by user name", response = User.class, position = 0)
  @ApiResponses(value = {
    @ApiResponse(code = 400, message = "Invalid username supplied"),
    @ApiResponse(code = 404, message = "User not found")})
  public Response getUserByName(
    @ApiParam(value = "The name that needs to be fetched. Use user1 for testing. ", required = true) @PathParam("username") String username)
    throws ApiException {
    User user = userData.findUserByName(username);
    if (user != null) {
      return Response.ok().entity(user).build();
    } else {
      throw new NotFoundException(404, "User not found");
    }
  }

  @GET("/user/login")
  @ApiOperation(value = "Logs user into the system", response = String.class, position = 6)
  @ApiResponses(value = {@ApiResponse(code = 400, message = "Invalid username/password supplied")})
  public Response loginUser(
    @ApiParam(value = "The user name for login", required = true) @Param("username") String username,
    @ApiParam(value = "The password for login in clear text", required = true) @Param("password") String password) {
    return Response.ok().entity("logged in user session:" + System.currentTimeMillis()).build();
  }

  @GET("/user/logout")
  @ApiOperation(value = "Logs out current logged in user session", position = 7)
  public Response logoutUser() {
    return Response.ok().entity("").build();
  }
}
