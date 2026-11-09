package es.test.manager.infraestructure.adapter.out.persistence;

import es.test.manager.domain.model.Project;
import es.test.manager.domain.port.out.ProjectRepository;
import es.test.manager.infraestructure.mapper.ProjectMapper;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** JPA adapter implementing the {@link ProjectRepository} outbound port. */
@Repository
@RequiredArgsConstructor
public class ProjectRepositoryAdapter implements ProjectRepository {

    private final ProjectJpaRepository projectJpaRepository;
    private final ProjectMapper projectMapper;

    @Override
    public Project save(Project project) {
        return projectMapper.toDomain(projectJpaRepository.save(projectMapper.toEntity(project)));
    }

    @Override
    public Optional<Project> findByIdAndUserId(Long id, Long userId) {
        return projectJpaRepository.findByIdAndUserId(id, userId).map(projectMapper::toDomain);
    }

    @Override
    public List<Project> findAllByUserIdOrderByCreatedAtDescIdDesc(Long userId) {
        return projectJpaRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(projectMapper::toDomain)
                .toList();
    }

    @Override
    public long countByUserId(Long userId) {
        return projectJpaRepository.countByUserId(userId);
    }

    @Override
    public void deleteById(Long id) {
        projectJpaRepository.deleteById(id);
    }
}
