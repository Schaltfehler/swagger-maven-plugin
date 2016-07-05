package com.wordnik.avans;

import com.wordnik.sample.JavaRestResourceUtil;
import com.wordnik.sample.data.StoreData;
import com.wordnik.sample.exception.NotFoundException;
import com.wordnik.sample.model.Order;
import io.swagger.annotations.*;
import me.geso.avans.annotation.GET;
import me.geso.avans.annotation.POST;
import me.geso.avans.annotation.Param;
import me.geso.avans.annotation.PathParam;

import javax.ws.rs.core.Response;

@Api(value = "store", produces = "application/json")

public class PetStoreResource {
  static StoreData storeData = new StoreData();
  static JavaRestResourceUtil ru = new JavaRestResourceUtil();

  @GET("/store/order/{orderId}")
  @ApiOperation(value = "Find purchase order by ID", notes = "For valid response try integer IDs with value <= 5 or > 10. Other values will generated exceptions", response = Order.class)
  @ApiResponses(value = {@ApiResponse(code = 400, message = "Invalid ID supplied"),
    @ApiResponse(code = 404, message = "Order not found")})
  public Response getOrderById(
    @ApiParam(hidden = true, value = "this is a hidden parameter", required = false) @Param("hiddenParam") String hiddenParam,
    @ApiParam(value = "ID of pet that needs to be fetched", allowableValues = "range[1,5]", required = true) @PathParam("orderId") String orderId)
    throws NotFoundException {
    Order order = storeData.findOrderById(ru.getLong(0, 10000, 0, orderId));
    if (order != null) {
      return Response.ok().entity(order).build();
    } else {
      throw new NotFoundException(404, "Order not found");
    }
  }

  @POST("/store/order")
  @ApiOperation(value = "Place an order for a pet")
  @ApiResponses({@ApiResponse(code = 400, message = "Invalid Order")})
  public Order placeOrder(
    @ApiParam(value = "order placed for purchasing the pet", required = true) Order order) {
    storeData.placeOrder(order);
    return storeData.placeOrder(order);
  }

  @POST("/store/pingPost")
  @ApiOperation(value = "Simple ping endpoint")
  @ApiResponses({
    @ApiResponse(code = 200, message = "Successful request - see response for 'pong'", response = String.class)})
  public String pingPost() {
    return "pingPost";
  }

  @GET("/store/pingGet")
  @ApiOperation(value = "Simple ping endpoint")
  @ApiResponses({
    @ApiResponse(code = 200, message = "Successful request - see response for 'pong'", response = String.class)})
  public String pingGet() {
    return "pong";
  }
}
