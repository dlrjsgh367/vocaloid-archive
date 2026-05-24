package com.vocaloidarchive.share.infra;

import com.vocaloidarchive.common.exception.BusinessException;
import com.vocaloidarchive.common.exception.ErrorCode;
import com.vocaloidarchive.share.application.dto.CardRenderRequest;
import com.vocaloidarchive.share.application.port.CardRendererPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Calls the standalone Playwright renderer container over HTTP. */
@Component
public class HttpCardRenderer implements CardRendererPort {

  private final RestClient client;

  public HttpCardRenderer(
      @Value("${RENDERER_BASE_URL:http://localhost:3000}") String baseUrl) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(3000);
    factory.setReadTimeout(8000);
    this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
  }

  @Override
  public byte[] render(CardRenderRequest request) {
    try {
      byte[] png = client.post()
          .uri("/render")
          .contentType(MediaType.APPLICATION_JSON)
          .body(request)
          .retrieve()
          .body(byte[].class);
      if (png == null || png.length == 0) {
        throw new IllegalStateException("renderer returned empty body");
      }
      return png;
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.CARD_RENDER_FAILED);
    }
  }
}
