package com.podo.domain.cs.service;

import com.podo.domain.cs.dto.request.FaqCreateRequest;
import com.podo.domain.cs.dto.request.FaqUpdateRequest;
import com.podo.domain.cs.dto.response.FaqResponse;
import com.podo.domain.cs.entity.Faq;
import com.podo.domain.cs.exception.FaqNotFoundException;
import com.podo.domain.cs.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FaqService {

    private final FaqRepository faqRepository;

    public List<FaqResponse> findAll() {
        return faqRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .map(FaqResponse::from)
                .toList();
    }

    @Transactional
    public FaqResponse create(FaqCreateRequest request) {
        Faq faq = faqRepository.save(Faq.builder()
                .category(request.getCategory())
                .question(request.getQuestion())
                .answer(request.getAnswer())
                .displayOrder(request.getDisplayOrder())
                .build());

        return FaqResponse.from(faq);
    }

    @Transactional
    public FaqResponse update(Long id, FaqUpdateRequest request) {
        Faq faq = findFaqById(id);
        faq.update(request.getCategory(), request.getQuestion(), request.getAnswer(), request.getDisplayOrder());
        return FaqResponse.from(faq);
    }

    @Transactional
    public void delete(Long id) {
        Faq faq = findFaqById(id);
        faqRepository.delete(faq);
    }

    public FaqResponse findById(Long id) {
        return FaqResponse.from(findFaqById(id));
    }

    private Faq findFaqById(Long id) {
        return faqRepository.findById(id)
                .orElseThrow(() -> new FaqNotFoundException(id));
    }
}
