package es.test.manager.domain.port.out;

import es.test.manager.domain.model.Tag;
import java.util.Collection;
import java.util.List;

/**
 * Outbound port: global tag persistence. Tags are reused by normalized name and never deleted when
 * a test case disappears (RF-058 a RF-066).
 */
public interface TagRepository {

    /** Returns the tags whose normalized name is in the given collection, in no particular order. */
    List<Tag> findByNormalizedNames(Collection<String> normalizedNames);

    /**
     * Persists the given new tags and flushes, so a concurrent UNIQUE(normalized_name) violation
     * surfaces here and can be translated to a 409 (RF-019, RF-039, RF-066).
     */
    List<Tag> saveAll(List<Tag> tags);
}
