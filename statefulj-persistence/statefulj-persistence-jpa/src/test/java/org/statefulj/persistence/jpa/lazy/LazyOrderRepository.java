package org.statefulj.persistence.jpa.lazy;

import org.springframework.data.repository.Repository;

public interface LazyOrderRepository extends Repository<LazyOrder, Long> {

    LazyOrder save(LazyOrder order);

    LazyOrder findOne(Long id);
}
