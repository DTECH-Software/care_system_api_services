/**
 * User: Himal_J
 * Date: 2/27/2025
 * Time: 7:53 AM
 * <p>
 */

package com.dtech.document.mapper.EntityToDto;

import com.dtech.document.dto.response.DocumentDownloadResponseDTO;
import com.dtech.document.model.Document;
import com.google.gson.Gson;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ImageDownloadMapper {
    private static final Gson gson = new Gson();

    public static DocumentDownloadResponseDTO  imageDownloadMapper(Document document) {
        try {
            log.info("Document download mapper {} ",document);
            return gson.fromJson(gson.toJson(document), DocumentDownloadResponseDTO.class);
        }catch (Exception e) {
            log.error(e);
            throw e;
        }
    }

}
