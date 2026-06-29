package com.team6.moduply.binarycontent.storage.local;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

class LocalFileResourceConfigTest {

  @Test
  @DisplayName("로컬 파일 리소스 설정 시 urlPrefix와 rootPath를 정규화하여 매핑한다.")
  void addResourceHandlers_success_with_normalized_properties() {
    // given
    LocalStorageProperties properties = new LocalStorageProperties();
    properties.setRootPath("./uploads/");
    properties.setUrlPrefix("/uploads/");

    LocalFileResourceConfig config = new LocalFileResourceConfig(properties);
    ResourceHandlerRegistry registry = mock(ResourceHandlerRegistry.class);
    ResourceHandlerRegistration registration = mock(ResourceHandlerRegistration.class);
    given(registry.addResourceHandler("/uploads/**")).willReturn(registration);

    // when
    config.addResourceHandlers(registry);

    // then
    verify(registry).addResourceHandler("/uploads/**");
    verify(registration).addResourceLocations("file:./uploads/");
  }
}
