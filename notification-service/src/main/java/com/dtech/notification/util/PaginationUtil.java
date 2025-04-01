/**
 * User: Himal_J
 * Date: 3/14/2025
 * Time: 9:44 AM
 * <p>
 */

package com.dtech.notification.util;


import com.dtech.notification.dto.request.PaginationRequest;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Log4j2
public class PaginationUtil {

    public static Pageable getPageable(PaginationRequest paginationRequest) {
        log.info("Pagination call {} ", paginationRequest);
        return PageRequest.of(paginationRequest.getPage(), paginationRequest.getSize(),paginationRequest.getSortDirection(),paginationRequest.getSortColumn());
    }

}
