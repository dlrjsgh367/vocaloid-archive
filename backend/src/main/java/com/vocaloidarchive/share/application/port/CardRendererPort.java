package com.vocaloidarchive.share.application.port;

import com.vocaloidarchive.share.application.dto.CardRenderRequest;

/** Renders a card payload to a PNG. Implementations throw {@code CARD_RENDER_FAILED} on failure. */
public interface CardRendererPort {
  byte[] render(CardRenderRequest request);
}
