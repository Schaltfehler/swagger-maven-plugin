package com.github.kongchen.swagger.docgen.reader;

import com.github.kongchen.swagger.docgen.GenerateException;
import io.swagger.annotations.*;
import io.swagger.converter.ModelConverters;
import io.swagger.models.*;
import io.swagger.models.Tag;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import me.geso.avans.annotation.GET;
import me.geso.avans.annotation.POST;
import org.apache.maven.plugin.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

public class AvansReader extends AbstractReader implements ClassSwaggerReader {
  private static final Logger LOGGER = LoggerFactory.getLogger(AvansReader.class);

  public AvansReader(Swagger swagger, Log LOG) {
    super(swagger, LOG);
  }

  @Override
  public Swagger read(Set<Class<?>> classes) throws GenerateException {
    for (Class cls : classes) {
      read(cls);
    }

    return swagger;
  }

  public Swagger getSwagger() {
    return swagger;
  }

  public Swagger read(Class cls) {
    return read(cls, "", null, false, new String[0], new String[0], new HashMap<String, Tag>(), new ArrayList<Parameter>());
  }

  protected Swagger read(Class<?> clazz, String parentPath, String parentMethod, boolean readHidden,
                         String[] parentConsumes, String[] parentProduces, Map<String, Tag> parentTags,
                         List<Parameter> parentParameters) {
    if (swagger == null) {
      swagger = new Swagger();
    }

    Api api = AnnotationUtils.findAnnotation(clazz, Api.class);

    if (parentConsumes.length == 0 && !api.consumes().isEmpty()) {
      parentConsumes = api.consumes().split(",");
    }
    if (parentProduces.length == 0 && !api.produces().isEmpty()) {
      parentProduces = api.produces().split(",");
    }

    if (!canReadApi(readHidden, api)) {
      return swagger;
    }

    Map<String, Tag> tags = updateTagsForApi(parentTags, api);
    List<SecurityRequirement> securities = getSecurityRequirements(api);

    // merge consumes, produces
    // look for method-level annotated properties
    // handle subresources by looking at return type

    // parse the method
    for (Method method : clazz.getMethods()) {
      ApiOperation apiOperation = AnnotationUtils.findAnnotation(method, ApiOperation.class);
      if (apiOperation == null || apiOperation.hidden()) {
        continue;
      }
      GET getMethod = AnnotationUtils.findAnnotation(method, GET.class);
      POST postMethod = AnnotationUtils.findAnnotation(method, POST.class);

      String operationPath = null;

      if (getMethod != null) {
        operationPath = getMethod.value();
      } else if (postMethod != null) {
        operationPath = postMethod.value();
      }

      if (operationPath != null) {
        Map<String, String> regexMap = new HashMap<String, String>();
        operationPath = parseOperationPath(operationPath, regexMap);

        String httpMethod = extractOperationMethod(apiOperation, method);

        Operation swaggerOperation = parseMethod(method, apiOperation);
        updateOperationParameters(parentParameters, regexMap, swaggerOperation);
        updateOperationProtocols(apiOperation, swaggerOperation);

        String[] apiOperationConsumes = new String[0];
        String[] apiOperationProduces = new String[0];

        if (!apiOperation.consumes().isEmpty()) {
          apiOperationConsumes = apiOperation.consumes().split(",");
        }
        if (!apiOperation.produces().isEmpty()) {
          apiOperationProduces = apiOperation.produces().split(",");
        }

        apiOperationConsumes = updateOperationConsumes(parentConsumes, apiOperationConsumes, swaggerOperation);
        apiOperationProduces = updateOperationProduces(parentProduces, apiOperationProduces, swaggerOperation);

        handleSubResource(apiOperationConsumes, httpMethod, apiOperationProduces, tags, method, operationPath, swaggerOperation);

        // can't continue without a valid http method
        httpMethod = (httpMethod == null) ? parentMethod : httpMethod;
        updateTagsForOperation(swaggerOperation, apiOperation);
        updateOperation(apiOperationConsumes, apiOperationProduces, tags, securities, swaggerOperation);
        updatePath(operationPath, httpMethod, swaggerOperation);
      }
    }

    return swagger;
  }

  private void handleSubResource(String[] apiConsumes, String httpMethod, String[] apiProduces,
                                 Map<String, Tag> tags, Method method, String operationPath, Operation operation) {
    if (isSubResource(method)) {
      Class<?> responseClass = method.getReturnType();
      Swagger subSwagger = read(responseClass, operationPath, httpMethod, true, apiConsumes, apiProduces, tags, operation.getParameters());
    }
  }

  protected boolean isSubResource(Method method) {
    Class<?> responseClass = method.getReturnType();
    return (responseClass != null) && (AnnotationUtils.findAnnotation(responseClass, Api.class) != null);
  }

  public Operation parseMethod(Method method, ApiOperation apiOperation) {
    Operation swaggerOperation = new Operation();

    String methodName = method.getName();
    String responseContainer = null;

    Class<?> responseClass = null;
    Map<String, Property> defaultResponseHeaders = null;

    if (apiOperation != null) {
      if (apiOperation.hidden()) {
        return null;
      }

      if (!apiOperation.nickname().isEmpty()) {
        methodName = apiOperation.nickname();
      }

      defaultResponseHeaders = parseResponseHeaders(apiOperation.responseHeaders());

      // Summary
      swaggerOperation.summary(apiOperation.value()).description(apiOperation.notes());

      // Custom Extension
      Set<Map<String, Object>> customExtensions = parseCustomExtensions(apiOperation.extensions());
      if (customExtensions != null) {
        for (Map<String, Object> extension : customExtensions) {
          if (extension == null) {
            continue;
          }
          for (Map.Entry<String, Object> map : extension.entrySet()) {
            swaggerOperation.setVendorExtension(map.getKey().startsWith("x-") ? map.getKey()
              : "x-" + map.getKey(), map.getValue());
          }
        }
      }


      if (!apiOperation.response().equals(Void.class)) {
        responseClass = apiOperation.response();
      }
      if (!apiOperation.responseContainer().isEmpty()) {
        responseContainer = apiOperation.responseContainer();
      }

      // Security
      List<SecurityRequirement> securities = new ArrayList<SecurityRequirement>();
      for (Authorization auth : apiOperation.authorizations()) {
        if (!auth.value().isEmpty()) {
          SecurityRequirement security = new SecurityRequirement();
          security.setName(auth.value());
          for (AuthorizationScope scope : auth.scopes()) {
            if (!scope.scope().isEmpty()) {
              security.addScope(scope.scope());
            }
          }
          swaggerOperation.security(security);
        }
      }
    }
    // Method Name
    swaggerOperation.operationId(methodName);

    if (responseClass == null) {
      // pick out response from method declaration
      LOGGER.debug("picking up response class from method " + method);
      Type genericReturnType = method.getGenericReturnType();
      responseClass = method.getReturnType();

      if (!responseClass.equals(Void.class) && !responseClass.equals(void.class)
        && (AnnotationUtils.findAnnotation(responseClass, Api.class) == null)) {
        LOGGER.debug("reading model " + responseClass);
        Map<String, Model> models = ModelConverters.getInstance().readAll(genericReturnType);
        LOGGER.debug(models.toString());
      }
    }

    // Response
    if ((responseClass != null)
      && !responseClass.equals(Void.class)
      && !responseClass.equals(javax.ws.rs.core.Response.class)
      && (AnnotationUtils.findAnnotation(responseClass, Api.class) == null)) {

      if (isPrimitive(responseClass)) {
        Property responseProperty;
        Property property = ModelConverters.getInstance().readAsProperty(responseClass);
        if (property != null) {
          if ("list".equalsIgnoreCase(responseContainer)) {
            responseProperty = new ArrayProperty(property);
          } else if ("map".equalsIgnoreCase(responseContainer)) {
            responseProperty = new MapProperty(property);
          } else {
            responseProperty = property;
          }
          swaggerOperation.response(apiOperation.code(), new Response().description("successful operation").schema(responseProperty).headers(defaultResponseHeaders));
        }
      } else if (!responseClass.equals(Void.class) && !responseClass.equals(void.class)) {

        Map<String, Model> models = ModelConverters.getInstance().read(responseClass);
        if (models.isEmpty()) {
          Property property = ModelConverters.getInstance().readAsProperty(responseClass);
          swaggerOperation.response(apiOperation.code(), new Response().description("successful operation").schema(property).headers(defaultResponseHeaders));
        }
        for (String key : models.keySet()) {
          Property responseProperty;

          if ("list".equalsIgnoreCase(responseContainer)) {
            responseProperty = new ArrayProperty(new RefProperty().asDefault(key));
          } else if ("map".equalsIgnoreCase(responseContainer)) {
            responseProperty = new MapProperty(new RefProperty().asDefault(key));
          } else {
            responseProperty = new RefProperty().asDefault(key);
          }
          swaggerOperation.response(apiOperation.code(), new Response().description("successful operation").schema(responseProperty).headers(defaultResponseHeaders));
          swagger.model(key, models.get(key));
        }
        models = ModelConverters.getInstance().readAll(responseClass);
        for (Map.Entry<String, Model> entry : models.entrySet()) {
          swagger.model(entry.getKey(), entry.getValue());
        }
      }
    }

    ApiResponses responseAnnotation = AnnotationUtils.findAnnotation(method, ApiResponses.class);
    if (responseAnnotation != null) {
      updateApiResponse(swaggerOperation, responseAnnotation);
    }

    if (swaggerOperation.getResponses() == null) {
      swaggerOperation.defaultResponse(new Response().description("successful operation"));
    }

    // Deprecated
    if (AnnotationUtils.findAnnotation(method, Deprecated.class) != null) {
      swaggerOperation.deprecated(true);
    }

    // Parameters
    Type[] genericParameterTypes = method.getGenericParameterTypes();
    Annotation[][] paramAnnotations = method.getParameterAnnotations();

    for (int i = 0; i < genericParameterTypes.length; i++) {
      Type type = genericParameterTypes[i];
      List<Annotation> annotations = Arrays.asList(paramAnnotations[i]);
      List<Parameter> parameters = getParameters(type, annotations);

      for (Parameter parameter : parameters) {
        swaggerOperation.parameter(parameter);
      }
    }

    // Process @ApiImplicitParams
    this.readImplicitParameters(method, swaggerOperation);

    return swaggerOperation;
  }

  public String extractOperationMethod(ApiOperation apiOperation, Method method) {
    if (!apiOperation.httpMethod().isEmpty()) {
      return apiOperation.httpMethod().toLowerCase();
    } else if (AnnotationUtils.findAnnotation(method, GET.class) != null) {
      return "get";
    } else if (AnnotationUtils.findAnnotation(method, POST.class) != null) {
      return "post";
    }
    return null;
  }

}
