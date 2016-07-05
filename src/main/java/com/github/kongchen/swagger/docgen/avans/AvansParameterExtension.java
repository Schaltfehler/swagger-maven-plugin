package com.github.kongchen.swagger.docgen.avans;

import io.swagger.converter.ModelConverters;
import io.swagger.jaxrs.ext.AbstractSwaggerExtension;
import io.swagger.jaxrs.ext.SwaggerExtension;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.parameters.SerializableParameter;
import io.swagger.models.properties.Property;
import me.geso.avans.annotation.Param;
import me.geso.avans.annotation.PathParam;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author chekong on 15/5/12.
 */
public class AvansParameterExtension extends AbstractSwaggerExtension {

  @Override
  public List<Parameter> extractParameters(List<Annotation> annotations, Type type, Set<Type> typesToSkip,
                                           Iterator<SwaggerExtension> chain) {
    if (shouldIgnoreType(type, typesToSkip)) {
      return new ArrayList<>();
    }

    List<Parameter> parameters = new ArrayList<Parameter>();
    SerializableParameter parameter = null;
    for (Annotation annotation : annotations) {
      parameter = getParameter(type, parameter, annotation);
    }
    if (parameter != null) {
      parameters.add(parameter);
    } else {
      if (chain.hasNext()) {
        return chain.next().extractParameters(annotations, type, typesToSkip, chain);
      }
    }

    return parameters;
  }

  public static SerializableParameter getParameter(Type type, SerializableParameter parameter,
                                                   Annotation annotation) {
    String defaultValue = "";

    if (annotation instanceof Param) {
      Param param = (Param) annotation;
      QueryParameter queryParameter = new QueryParameter().name(param.value());

      if (!defaultValue.isEmpty()) {
        queryParameter.setDefaultValue(defaultValue);
      }
      Property schema = ModelConverters.getInstance().readAsProperty(type);
      if (schema != null) {
        queryParameter.setProperty(schema);
      }

      String parameterType = queryParameter.getType();
      if (parameterType == null || parameterType.equals("ref")) {
        queryParameter.setType("string");
      }
      parameter = queryParameter;
    } else if (annotation instanceof PathParam) {
      PathParam param = (PathParam) annotation;
      PathParameter pathParameter = new PathParameter().name(param.value());
      if (!defaultValue.isEmpty()) {
        pathParameter.setDefaultValue(defaultValue);
      }
      Property schema = ModelConverters.getInstance().readAsProperty(type);
      if (schema != null) {
        pathParameter.setProperty(schema);
      }

      String parameterType = pathParameter.getType();
      if (parameterType == null || parameterType.equals("ref")) {
        pathParameter.setType("string");
      }
      parameter = pathParameter;
    }

    //fix parameter type issue, try to access parameter's type
    if (parameter != null) {
      try {
        Field t = parameter.getClass().getDeclaredField("type");
        t.setAccessible(true);
        Object tval = t.get(parameter);
        if (tval.equals("ref")) {
          //fix to string
          t.set(parameter, "string");
        }
        t.setAccessible(false);
      } catch (NoSuchFieldException e) {
        //ignore
      } catch (IllegalAccessException e) {
        //ignore
      }
    }
    return parameter;
  }
}
