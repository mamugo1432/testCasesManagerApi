package es.test.manager.infraestructure.mapper;

import es.test.manager.domain.model.User;
import es.test.manager.infraestructure.adapter.out.persistence.UserJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/** MapStruct mapper between the user JPA entity and the domain model. */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    User toDomain(UserJpaEntity entity);

    UserJpaEntity toEntity(User user);
}
