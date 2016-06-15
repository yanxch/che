package org.eclipse.che.api.factory.server;

import org.eclipse.che.api.factory.server.model.impl.FactoryImpl;
import org.eclipse.che.api.factory.shared.dto.FactoryDto;
import org.eclipse.che.dto.server.DtoFactory;

/**
 * Helps to convert to DTOs related to factory.
 *
 * @author Sergii Leschenko
 */
public final class DtoConverter {
    public static FactoryDto asDto(FactoryImpl factory) {
        return DtoFactory.newDto(FactoryDto.class);
    }

    private DtoConverter() {}
}
