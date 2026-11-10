package es.test.manager.application.mapper;

import es.test.manager.application.dto.auth.UserResponse;
import es.test.manager.domain.model.AuthenticatedUser;
import es.test.manager.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/** MapStruct mapper from the user models to the public DTO, which never exposes the password hash. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserDtoMapper {

    UserResponse toResponse(User user);

    UserResponse toResponse(AuthenticatedUser user);
}
