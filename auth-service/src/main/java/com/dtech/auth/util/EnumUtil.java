/**
 * User: Himal_J
 * Date: 3/10/2025
 * Time: 12:11 PM
 * <p>
 */

package com.dtech.auth.util;

import com.dtech.auth.dto.SimpleBaseDTO;
import com.dtech.auth.enums.DescribableEnum;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;
import java.util.List;

@Log4j2
public class EnumUtil {

    public static <E extends Enum<E> & DescribableEnum> List<SimpleBaseDTO> getEnumList(Class<E> enumClass) {
        log.info("Enum class simple base mapper {} ", enumClass.getName());
        return Arrays.stream(enumClass.getEnumConstants())
                .map(enumValue -> new SimpleBaseDTO(enumValue.name(), enumValue.getDescription()))
                .toList();
    }
}
