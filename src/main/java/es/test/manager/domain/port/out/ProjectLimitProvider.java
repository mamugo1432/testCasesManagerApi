package es.test.manager.domain.port.out;

/** Outbound port: maximum number of projects a single user may own (RF-013). */
public interface ProjectLimitProvider {

    int maxProjectsPerUser();
}
