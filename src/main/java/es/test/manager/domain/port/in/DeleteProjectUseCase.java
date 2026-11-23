package es.test.manager.domain.port.in;

/** Inbound port: delete one project by id (RF-022, RF-025, RF-027). */
public interface DeleteProjectUseCase {

    void delete(Long id);
}
