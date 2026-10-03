package com.pm.appointmentservice.config;

import com.pm.appointmentservice.security.CurrentUser;
import com.pm.appointmentservice.security.CurrentUserArgumentResolver;
import com.pm.appointmentservice.security.RoleCheckInterceptor;
import java.util.List;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  static {
    SpringDocUtils.getConfig().addRequestWrapperToIgnore(CurrentUser.class);
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new RoleCheckInterceptor());
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(new CurrentUserArgumentResolver());
  }
}
