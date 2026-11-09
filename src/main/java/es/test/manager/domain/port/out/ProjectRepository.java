package es.test.manager.domain.port.out;

import es.test.manager.domain.model.Project;
import java.util.List;
import java.util.Optional;

/** Outbound port: project persistence. Every lookup is scoped by owner (RF-003). */
public interface ProjectRepository {

    Project save(Project project);

    Optional<Project> findByIdAndUserId(Long id, Long userId);

    /** Projects of one user, newest first; ties are broken by id descending (RF-017). */
    List<Project> findAllByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    long countByUserId(Long userId);

    void deleteById(Long id);
}
