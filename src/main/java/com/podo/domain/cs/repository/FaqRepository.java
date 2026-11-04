package com.podo.domain.cs.repository;

import com.podo.domain.cs.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaqRepository extends JpaRepository<Faq, Long> {
    List<Faq> findAllByOrderByDisplayOrderAscIdAsc();
}
