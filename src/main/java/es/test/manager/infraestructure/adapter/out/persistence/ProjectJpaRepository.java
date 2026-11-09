package es.test.manager.infraestructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link ProjectJpaEntity}. Every lookup is scoped by owner. */
public interface ProjectJpaRepository extends JpaRepository<ProjectJpaEntity, Long> {

    Optional<ProjectJpaEntity> findByIdAndUserId(Long id, Long userId);

    List<ProjectJpaEntity> findAllByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    long countByUserId(Long userId);
}
