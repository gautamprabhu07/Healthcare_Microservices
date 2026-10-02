package com.pm.billingservice.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restricts an endpoint (or every endpoint in a controller) to the given
 * roles. With no roles, any authenticated user is allowed. Endpoints without
 * this annotation are not checked at all. Identity comes from the X-User-*
 * headers set by the API gateway.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiredRole {

  Role[] value() default {};
}
